package koma.controller.sync

import koma.Failure
import koma.matrix.MatrixApi
import koma.matrix.sync.SyncResponse
import koma.util.testFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import kotlin.time.MonoClock
import kotlin.time.seconds
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

/**
 * get events using the sync api
 * it stops when there is an exception
 * it is up to the caller to restart the sync
 */
@Deprecated("use flows")
class MatrixSyncReceiver(private val client: MatrixApi, var since: String?
) {
    /**
     * channel of responses from the sync api
     */
    val events = Channel<Result<SyncResponse, Failure>>(3)

    suspend fun startSyncing() = coroutineScope {
        var timeout = 50.seconds
        launch {
            while (true) {
                val startTime = MonoClock.markNow()
                val (it, e, res) = client.sync(since, timeout = timeout)
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
        }
    }
}
