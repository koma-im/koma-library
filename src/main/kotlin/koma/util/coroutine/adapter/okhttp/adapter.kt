package koma.util.coroutine.adapter.okhttp

import koma.Failure
import koma.HttpFailure
import koma.IOFailure
import koma.InvalidData
import koma.util.KResult as Result
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import kotlin.coroutines.resume


/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 */
suspend fun Call.await(): Result<Response, Failure> {
    return suspendCancellableCoroutine { continuation ->
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

        registerOnCompletion(continuation)
    }
}

fun Response.extract(): Result<ResponseBody, Failure> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body == null) Result.failure(InvalidData("Response body is null"))
        else Result.of(body)
    } else {
        body()?.close()
        Result.failure(HttpFailure(this.code(), this.message()))
    }
}

private fun Call.registerOnCompletion(continuation: CancellableContinuation<*>) {
    continuation.invokeOnCancellation {
        if (continuation.isCancelled)
            try {
                cancel()
            } catch (ex: Throwable) {
                //Ignore cancel exception
            }
    }
}
