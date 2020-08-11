package koma

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.ResponseException
import io.ktor.client.request.HttpResponseData
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.readText
import koma.matrix.json.jsonDefault
import koma.matrix.user.auth.Unauthorized
import koma.util.KResult
import koma.util.coroutine.adapter.retrofit.toMatrixFailure
import koma.util.given
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
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
class OtherFailure(message: String
                   , val cause: Throwable?=null
): KomaFailure(message) {
    override fun toString() = "OtherFailure, $message"
}

open class HttpFailure(val status: HttpStatusCode,
                       val body: String? = null,
                       val cause: Throwable?=null
): KomaFailure("HTTP $status") {
    val http_code: Int
        get() =status.value
    val http_message: String
        get() = status.description
    override fun toString() = buildString {
        append("HTTP")
        append(' ')
        append(http_code)
        append(' ')
        append(http_message)
        body?.let {
            append(' ')
            append(it)
        }
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
        val more: Map<String, JsonElement>,
        status: HttpStatusCode,
        http_body: String?
): HttpFailure(status, body = http_body) {
    override fun toString() = buildString {
        append("MatrixFailure(")
        append(errcode)
        append(": ")
        append(error)
        for ((key, value) in more) {
            if (value !is JsonPrimitive) continue
            append(", ")
            append(key)
            append(": ")
            val v = value.contentOrNull ?: value.longOrNull ?: value.doubleOrNull ?: value
            append(v)
        }
        append(')')
    }
}

class AuthFailure(
        val fail: Unauthorized,
        httpStatusCode: HttpStatusCode
): HttpFailure(httpStatusCode)

suspend fun Throwable.toFailure(): KomaFailure {
    return when (this) {
        is SocketTimeoutException -> Timeout(cause = this)
        is SerializationException -> InvalidData(cause=this)
        is NoTransformationFoundException -> InvalidData("Reponse may lack correct Content-Type",
                this)
        is IOException -> IOFailure(this)
        is ResponseException -> parseResponseFailure(this)
        else -> OtherFailure("$this", cause = this)
    }
}

private suspend fun parseResponseFailure(responseException: ResponseException): HttpFailure {
    val response = responseException.response
    val body = runCatching {
        val packet = response.receive<Input>()
        packet.readText(charset = Charsets.UTF_8, max=65536)
    }.getOrNull()
    if (body != null) {
        val mf = runCatching {
            jsonDefault.decodeFromString(JsonObjectSerializer, body)
        }.getOrNull()?.toMatrixFailure(response.status, body)
        if (mf != null) return mf
    }
    return HttpFailure(response.status, body = body, cause = responseException)
}