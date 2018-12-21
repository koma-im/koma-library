package koma.controller.sync

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success
import koma.matrix.sync.SyncResponse
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import java.net.SocketTimeoutException
import java.time.Instant


private val logger = KotlinLogging.logger {}

val longPollTimeout = 50

/**
 * detect computer suspend and resume and restart sync
 */
fun detectTimeLeap(): Channel<Unit> {
    val timeleapSignal = Channel<Unit>(Channel.CONFLATED)
    var prev = Instant.now().epochSecond
    GlobalScope.launch {
        while (true) {
            delay(1000000) // should be 1 sec
            val now = Instant.now().epochSecond
            if (now - prev > 2) {
                logger.info { "System time leapt from $prev to $now" }
                timeleapSignal.send(Unit)
            }
            prev = now
        }
    }
    return timeleapSignal
}


/**
 * get events using the sync api
 * it stops when there is an exception
 * it is up to the caller to restart the sync
 */
class MatrixSyncReceiver(var since: String?=null) {
    /**
     * channel of responses from the sync api
     */
    val events = Channel<Result<SyncResponse, Exception>>(3)
    /**
     * check whether the computer was not running for some time
     */
    private val timeCheck = detectTimeLeap()
    /**
     * stop syncing, returns true when it's stopped
     */
    private val shutdownChan = Channel<CompletableDeferred<Boolean>>()

    suspend fun stopSyncing() {
        val complete = CompletableDeferred<Boolean>()
        shutdownChan.send(complete)
        complete.await()
    }
    fun startSyncing() {
        val client = appState.apiClient

        GlobalScope.launch {
            sync@ while (true) {
                val apiRes = async { client.getEvents(since).awaitMatrix() }
                val ss = select<SyncStatus> {
                    apiRes.onAwait { SyncStatus.Response(it) }
                    shutdownChan.onReceive { i -> SyncStatus.Shutdown(i) }
                    timeCheck.onReceive { SyncStatus.Resync() }
                }
                when (ss) {
                    is SyncStatus.Shutdown -> {
                        logger.info { "shutting down sync" }
                        apiRes.cancel()
                        ss.done.complete(true)
                        break@sync
                    }
                    is SyncStatus.Response -> {
                        ss.response.success {
                            since = it.next_batch
                            client.next_batch = since
                        }
                        if (ss.response is Result.Failure) {
                            val e = ss.response.error
                            if (e is SocketTimeoutException) {
                                logger.warn { "timeout during sync: $e" }
                                delay(1500)
                                logger.info { "resuming sync after timeout" }
                                continue@sync
                            } else {
                                logger.warn { "Exception during sync: ${e}" }
                            }
                        }
                        events.send(ss.response)
                    }
                    is SyncStatus.Resync -> {
                        logger.info { "Restarting sync" }
                        apiRes.cancel()
                    }
                }
            }
        }
    }
}
