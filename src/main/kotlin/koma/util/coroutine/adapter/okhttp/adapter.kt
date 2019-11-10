package koma.util.coroutine.adapter.okhttp

import koma.*
import koma.matrix.json.MoshiInstance
import koma.matrix.json.deserialize
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

/**
 * await and deserialize json
 */
internal suspend inline fun <reified T : Any> Call.awaitType(): Result<T, KomaFailure> {
    val (response, failure, result) = this.await()
    if (result.testFailure(response, failure)) return Result.failure(failure)
    val body = response.body()
    return coroutineScope {
        if (!response.isSuccessful) {
            val matrixFailure = body?.use {
                it.source().use {
                    deserialize<Map<String, Any>>(it).getOrNull()
                }
            }?.toMatrixFailure(response.code(), response.message())
            val f = matrixFailure ?: HttpFailure(response.code(), response.message())
            KResult.failure(f)
        } else if (body == null) {
            KResult.failure(InvalidData("Response body is null"))
        } else {
            body.use {it.source().use {
                deserialize<T>(it)
            } }
        }
    }
}
