package koma

import com.squareup.moshi.JsonDataException
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.features.ClientRequestException
import koma.matrix.user.auth.Unauthorized
import koma.util.KResult
import koma.util.given
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.JsonDecodingException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.time.Duration

open class Failure(val message: String? = null)

internal typealias KResultF<T> = KResult<T, KomaFailure>

sealed class KomaFailure(message: String?= null): Failure(message) {
    override fun toString() = "KomaFailure($message)"
}

class IOFailure(val throwable: Throwable): KomaFailure("IOError $throwable") {
    override fun toString(): String {
        return "IOError $throwable"
    }
}
class Timeout(val duration: Duration? = null
              , val cause: Throwable? = null
): KomaFailure("timeout $duration $cause") {
    override fun toString() = StringBuilder("Timeout").given(duration) {
        append(" after $it")
    }.given(cause) {
        append(" from $it")
    }.toString()
}

class InvalidData(message: String? = null, val cause: Throwable?=null): KomaFailure(message) {
    override fun toString() = buildString {
        append("InvalidData")
        given(message) { append(", $it")}
        given(cause) { append(", caused by $cause")}
    }
}
class OtherFailure(message: String): KomaFailure(message) {
    override fun toString() = "OtherFailure, $message"
}

open class HttpFailure(val http_code: Int,
                     val http_message: String,
                       val cause: Throwable?=null
): KomaFailure("HTTP $http_code $http_message") {
    override fun toString(): String {
        Result
        return "HTTP $http_code $http_message"
    }
}

class MatrixFailure(
        /**
         * M_
         */
        val errcode: String,
        val error: String,
        /**
         * additional fields
         */
        val more: Map<String, Any>,
        http_code: Int,
        http_message: String
): HttpFailure(http_code, http_message) {
    override fun toString() = "Matrix Error $errcode: $error"
}

class AuthFailure(val status: Unauthorized, http_code: Int, http_message: String
): HttpFailure(http_code, http_message)

fun Throwable.toFailure(): KomaFailure {
    return when (this) {
        is SocketTimeoutException -> Timeout(cause = this)
        is JsonDataException -> InvalidData("$this", this)
        is JsonDecodingException -> InvalidData(cause=this)
        is MissingFieldException -> InvalidData(cause = this)
        is NoTransformationFoundException -> InvalidData("Reponse may lack correct Content-Type",
                this)
        is IOException -> IOFailure(this)
        is ClientRequestException -> {
            val response = this.response
            HttpFailure(response.status.value, response.status.description, this)
        }
        else -> OtherFailure("$this")
    }
}