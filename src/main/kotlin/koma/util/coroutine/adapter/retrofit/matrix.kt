package koma.util.coroutine.adapter.retrofit

import koma.matrix.json.MoshiInstance
import koma.util.flatMap
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Response

import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

suspend fun <T : Any> Call<T>.awaitMatrix(): Result<T, Exception>
        = this.await().flatMap { it.extractBody() }


private fun<T: Any> Response<T>.extractBody(): Result<T, Exception> {
    return if (this.isSuccessful) {
        val body = this.body()
        if (body == null) Result.error(NullPointerException("Response body is null"))
        else Result.success(body)
    } else {
        val s = this.errorBody()?.source()?.readUtf8()
        val me = s?.let { MatrixError.fromString(it) }
        val e = if (me != null) {
            MatrixException(this.code(), this.message(), me, s)
        } else {
            HttpException(this.code(), this.message(), body = s)
        }
        Result.error(e)
    }
}

class MatrixException(code: Int, msg: String, val mxErr: MatrixError, body: String? = null)
    : Exception(msg)
{
    val httpException = HttpException(code, msg, body)
    val matrixErrorMessage by lazy { "Matrix Error ${mxErr.errcode} ${mxErr.error}" }
    val fullerErrorMessage by lazy {
        "$httpException\n" +
                "Matrix Error ${mxErr.errcode} ${mxErr.error}"
    }

    override fun toString(): String = matrixErrorMessage
}

class MatrixError(
        val errcode: String,
        val error: String
) {
    override fun toString() = "$errcode: $error"

    companion object {
        private val moshi = MoshiInstance.moshi
        private val jsonAdapter = moshi.adapter(MatrixError::class.java)

        fun fromString(s: String): MatrixError? {
            try {
                val e = jsonAdapter.fromJson(s)
                e ?: return null
                // moshi may disregard kotlin's non-nullability
                // and anything could be parsed as nulls
                if (e.errcode == null) {
                    return null
                }
                return e
            } catch (e: java.lang.Exception) {
                return null
            }
        }
    }
}
