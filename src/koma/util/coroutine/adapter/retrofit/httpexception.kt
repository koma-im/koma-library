package koma.util.coroutine.adapter.retrofit

import okhttp3.Response

open class HttpException(val code: Int,
                         override val message: String,
                         val body: String? = null
): Exception(message) {
    override fun toString(): String {
        return "HTTP $code $message"
    }
    companion object {
        fun fromOkhttp(response: Response): HttpException {
            response.body()?.close()
            val code = response.code()
            val message = response.message()
            return HttpException(code, message)
        }
    }
}
