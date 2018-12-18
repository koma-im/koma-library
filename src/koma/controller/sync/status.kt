package koma.controller.sync

import com.github.kittinunf.result.Result
import koma.matrix.sync.SyncResponse
import kotlinx.coroutines.CompletableDeferred

sealed class SyncStatus {
    class Resync(): SyncStatus()
    class Shutdown(val done: CompletableDeferred<Boolean>): SyncStatus()
    class Response(val response: Result<SyncResponse, Exception>): SyncStatus()
}
