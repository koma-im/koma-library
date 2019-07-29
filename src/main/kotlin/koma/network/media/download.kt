package koma.network.media

import koma.Failure
import koma.Koma
import koma.util.coroutine.adapter.okhttp.await
import koma.util.coroutine.adapter.okhttp.extract
import koma.util.getOr
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

suspend fun Koma.getResponse(url: HttpUrl): Result<ResponseBody, Failure> {
    val req = Request.Builder().url(url).build()
    val httpres = this.http.client.newCall(req).await() getOr { return Result.failure(it)}
    return httpres.extract()
}

suspend fun Server.downloadMedia(mhUrl: MHUrl): Result<ByteArray, KomaFailure> {
    val req = when (mhUrl) {
        is MHUrl.Mxc -> {
            val u = this.mxcToHttp(mhUrl)
            Request.Builder().url(u)
                    .cacheControl(CacheControl
                            .Builder()
                            .maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS)
                            .build())
                    .build()
        }
        is MHUrl.Http -> {
            var r = Request.Builder().url(mhUrl.http)
            if (mhUrl.maxStale != null) {
                r = r.cacheControl(CacheControl
                        .Builder()
                        .maxStale(mhUrl.maxStale, TimeUnit.SECONDS)
                        .build())
            }
            r.build()
        }
    }
    val bs = this.km.getHttpBytes(req)
    return bs
}

private suspend fun Koma.getHttpBytes(req: Request): Result<ByteArray, KomaFailure> {
    val hr = this.http.client.newCall(req).await()
    val body = hr.flatMap { it.extract() }
    val v = body.map { it.bytes() }
    return v
}

/**
 * matrix or http media url
 */
sealed class MHUrl {
    data class Mxc(val server: String, val media: String): MHUrl()
    data class Http(val http: HttpUrl,
               val maxStale: Int? = null): MHUrl()

    override fun toString(): String {
        return when (this) {
            is Http -> this.http.toString()
            is Mxc -> "mxc://$server/$media"
        }
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
