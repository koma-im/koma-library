package koma.util.coroutine.adapter.retrofit

import koma.*
import koma.matrix.json.jsonDefault
import koma.matrix.json.parseResult
import koma.util.getOr
import koma.util.getOrThrow
import koma.util.testFailure
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Response

import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}


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
    val (m, e, result) = jsonDefault.parseResult(JsonObject.serializer(), s)
    if (result.testFailure(m ,e)) {
        logger.warn { "Is not json, $s. Exception: $e" }
        return null
    }
    return m.toMatrixFailure(code, message)
}

internal fun Map<String, Any>.toMatrixFailure(code: Int, message: String): MatrixFailure? {
    val j: MutableMap<String, Any> = this as MutableMap<String, Any>
    val c = j.remove("errcode")?.toString() ?: return null
    val e = j.remove("error")?.toString() ?: return null
    return MatrixFailure(c, e, j, code, message)
}