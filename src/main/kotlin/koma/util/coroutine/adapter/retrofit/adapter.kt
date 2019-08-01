package koma.util.coroutine.adapter.retrofit

import koma.Failure
import koma.IOFailure
import koma.KomaFailure
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import koma.util.KResult as Result

/**
 * Suspend extension for [Call] that returns a result
 */
internal suspend fun <T : Any> Call<T>.await(): Result<Response<T>, KomaFailure> {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>?, response: Response<T>) {
                continuation.resume(Result.success(response))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resume(Result.failure(IOFailure(t)))
            }
        })
    }
}
