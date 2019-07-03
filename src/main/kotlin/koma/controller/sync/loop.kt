package koma.controller.sync

import koma.Failure
import koma.matrix.MatrixApi
import koma.matrix.sync.SyncResponse
import koma.util.onSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import java.time.Instant
import koma.util.KResult as Result

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
class MatrixSyncReceiver(private val client: MatrixApi, var since: String?) {
    /**
     * channel of responses from the sync api
     */
    val events = Channel<Result<SyncResponse, Failure>>(3)
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

        GlobalScope.launch {
            sync@ while (true) {
                val apiRes = async { client.asyncEvents(since) }
                val ss = select<SyncStatus> {
                    apiRes.onAwait { SyncStatus.Response(it) }
                    shutdownChan.onReceive { i -> SyncStatus.Shutdown(i) }
                    timeCheck.onReceive { SyncStatus.Resync() }
                }
                when (ss) {
                    is SyncStatus.Shutdown -> {
                        logger.info { "shutting down sync" }
                        apiRes.cancelAndJoin()
                        ss.done.complete(true)
                        break@sync
                    }
                    is SyncStatus.Response -> {
                        val res = ss.response
                        events.send(res)
                        res.onSuccess {
                            since = it.next_batch
                        }
                        val e = res.failureOrNull()
                        if (e != null) {
                            logger.warn { "Exception during sync: $e" }
                            break@sync
                        }
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
