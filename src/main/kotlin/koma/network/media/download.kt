package koma.network.media

import koma.Failure
import koma.util.coroutine.adapter.okhttp.await
import koma.util.coroutine.adapter.okhttp.extract
import koma.util.failureOrThrow
import koma.util.getOr
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

suspend fun getResponse(httpClient: OkHttpClient, url: HttpUrl): Result<ResponseBody, Failure> {
    val req = Request.Builder().url(url).build()
    val r = httpClient.newCall(req).await()
    val httpres =  r.getOrNull()?: return Result.failure(r.failureOrThrow())
    return httpres.extract()
}

typealias MxcOrHttp = MHUrl

/**
 * matrix or http media url
 */
sealed class MHUrl {
    data class Mxc(val server: String, val media: String): MHUrl() {
        override fun toString() = "mxc://$server/$media"
    }
    data class Http(val http: HttpUrl): MHUrl() {
        override fun toString() = this.http.toString()
    }
}

private const val prefix="mxc://"

fun String.parseMxc(): MHUrl? {
    if (indexOf(prefix)== 0) {
        val sep0 = indexOf('/', prefix.length)
        val end = indexOf('/', sep0+1).let {
            if (it > 0) it else length
        }
        val  server = substring(prefix.length, sep0)
        val  m = substring(sep0+1, end)
        return MHUrl.Mxc(server,m)
    } else {
        val h =HttpUrl.parse(this)?:return null
        return MHUrl.Http(h)
    }
}
