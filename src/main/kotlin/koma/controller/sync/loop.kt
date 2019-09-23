package koma.controller.sync

import koma.Failure
import koma.matrix.MatrixApi
import koma.matrix.sync.SyncResponse
import koma.util.onSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import java.net.SocketTimeoutException
import java.time.Instant
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

/**
 * detect computer suspend and resume and restart sync
 */
fun CoroutineScope.detectTimeLeap(): Channel<Unit> {
    val timeleapSignal = Channel<Unit>(Channel.CONFLATED)
    var prev = Instant.now().epochSecond
    launch {
        while (true) {
            delay(1000)
            val now = Instant.now().epochSecond
            if (now - prev > 20) {
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
class MatrixSyncReceiver(private val client: MatrixApi, var since: String?
) {
    /**
     * channel of responses from the sync api
     */
    val events = Channel<Result<SyncResponse, Failure>>(3)

    suspend fun startSyncing() = coroutineScope {
        // check whether the computer was not running for some time
        val timeCheck = detectTimeLeap()
        val longPollTimeout = 50000
        var poller = client.getEventPoller(longPollTimeout)
        launch {
            sync@ while (true) {
                val apiRes = async { poller.getEvent(since) }
                val ss: SyncStatus = select<SyncStatus> {
                    apiRes.onAwait { SyncStatus.Response(it) }
                    timeCheck.onReceive { SyncStatus.Resync() }
                }
                when (ss) {
                    is SyncStatus.Response -> {
                        val res = ss.response
                        events.send(res)
                        res.onSuccess {
                            if (poller.apiTimeout < longPollTimeout) {
                                poller = poller.withTimeout(longPollTimeout)
                            }
                            since = it.next_batch
                        }
                        val e = res.failureOrNull()
                        if (e != null) {
                            if (e is SocketTimeoutException) {
                                logger.warn { "Timeout during sync: $e" }
                                if (poller.apiTimeout > 1) {
                                    poller = poller.withTimeout(1)
                                }
                            } else {
                                logger.warn { "Exception during sync: $e" }
                                delay(10000)
                            }
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
