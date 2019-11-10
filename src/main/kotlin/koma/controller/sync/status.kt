package koma.controller.sync

import koma.Failure
import koma.KomaFailure
import koma.matrix.sync.SyncResponse
import kotlinx.coroutines.CompletableDeferred
import koma.util.KResult as Result

internal sealed class SyncStatus {
    class Resync(): SyncStatus()
    class Response(val response: Result<SyncResponse, KomaFailure>): SyncStatus()
}
