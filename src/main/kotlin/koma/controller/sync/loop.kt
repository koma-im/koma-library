package koma.controller.sync

import koma.Failure
import koma.matrix.MatrixApi
import koma.matrix.sync.SyncResponse
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import kotlin.time.MonoClock
import kotlin.time.seconds
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

/**
 * detect computer suspend and resume and restart sync
 */
fun CoroutineScope.detectTimeLeap(): Channel<Unit> {
    val timeleapSignal = Channel<Unit>(Channel.CONFLATED)
    var prev = MonoClock.markNow()
    launch {
        while (true) {
            delay(1000)
            val elapsed = prev.elapsedNow()
            val now = prev + elapsed
            if (elapsed.inSeconds > 20) {
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
        var timeout = 50.seconds
        launch {
            sync@ while (true) {
                val startTime = MonoClock.markNow()
                val apiRes = async { client.sync(since, timeout = timeout) }
                val ss: SyncStatus = select<SyncStatus> {
                    apiRes.onAwait { SyncStatus.Response(it) }
                    timeCheck.onReceive { SyncStatus.Resync() }
                }
                when (ss) {
                    is SyncStatus.Response -> {
                        val (it, e, res) = ss.response
                        events.send(res)
                        if (!res.testFailure(it, e)) {
                            timeout = 50.seconds
                            since = it.next_batch
                        }else {
                            timeout = 1.seconds
                            logger.warn { "Sync failure: $e" }
                            val minInterval = 1.seconds // limit rate of retries
                            val dur= startTime.elapsedNow()
                            if (dur < minInterval) {
                                val remaining = minInterval - dur
                                delay(remaining.toLongMilliseconds())
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
