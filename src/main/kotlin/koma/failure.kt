package koma

import koma.matrix.user.auth.Unauthorized
import okhttp3.Response

open class Failure(val message: String)

sealed class KomaFailure(message: String): Failure(message)

class IOFailure(val throwable: Throwable): KomaFailure("IOError $throwable") {
    override fun toString(): String {
        return "IOError $throwable"
    }
}
class InvalidData(message: String): KomaFailure(message) {
    override fun toString() = "InvalidData, $message"
}
class OtherFailure(message: String): KomaFailure(message) {
    override fun toString() = "OtherFailure, $message"
}

open class HttpFailure(val http_code: Int,
                     val http_message: String
): KomaFailure("HTTP $http_code $http_message") {
    override fun toString(): String {
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