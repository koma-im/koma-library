package koma.util.coroutine.adapter.okhttp

import koma.*
import koma.util.testFailure
import koma.util.KResult as Result
import koma.util.KResult
import koma.util.coroutine.adapter.retrofit.toMatrixFailure
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import kotlin.coroutines.resume


/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 */
suspend fun Call.await(): Result<Response, KomaFailure> {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                continuation.resume(Result.of (response))
            }

            override fun onFailure(call: Call, t: IOException) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(Result.failure(IOFailure(t)))
            }
        })
    }
}

fun Response.extract(): Result<ResponseBody, KomaFailure> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body == null) Result.failure(InvalidData("Response body is null"))
        else Result.of(body)
    } else {
        body()?.close()
        Result.failure(HttpFailure(this.code(), this.message()))
    }
}
