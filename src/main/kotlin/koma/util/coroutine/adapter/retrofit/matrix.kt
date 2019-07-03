package koma.util.coroutine.adapter.retrofit

import koma.*
import koma.matrix.json.MoshiInstance
import koma.util.flatMap
import koma.util.getOr
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Response

import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

internal suspend fun <T : Any> Call<T>.awaitMatrix(): Result<T, KomaFailure> {
    return this.await().flatMap { it.extractMatrix() }
}

/**
 * extract deserilized successful response or failure
 */
internal fun<T> Response<T>.extractMatrix(): Result<T, KomaFailure> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body == null) Result.failure(InvalidData("Response body is null"))
        else Result.success(body)
    } else {
        val e = this.errorBody()?.string()?.let {
            tryGetMatrixFailure(it, this.code(), this.message())
        }?: HttpFailure(this.code(), this.message())
        Result.failure(e)
    }
}

@Suppress("UNCHECKED_CAST")
private fun tryGetMatrixFailure(s: String, code: Int, message: String): MatrixFailure? {
    val m = MoshiInstance.mapAdapter.fromJson(s) ?: return null
    val j: MutableMap<String, Any> = m as MutableMap<String, Any>
    val c = j.remove("errcode")?.toString() ?: return null
    val e = j.remove("error")?.toString() ?: return null
    return MatrixFailure(c, e, j, code, message)
}