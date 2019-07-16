package koma.network.media

import koma.*
import koma.network.matrix.media.mxcToHttp
import koma.util.coroutine.adapter.okhttp.await
import koma.util.coroutine.adapter.okhttp.extract
import koma.util.flatMap
import koma.util.fold
import koma.util.getOr
import koma.util.map
import okhttp3.*
import koma.util.KResult as Result
import java.util.concurrent.TimeUnit

suspend fun Koma.getResponse(url: HttpUrl): Result<ResponseBody, Failure> {
    val req = Request.Builder().url(url).build()
    val httpres = this.http.client.newCall(req).await() getOr { return Result.failure(it)}
    return httpres.extract()
}

suspend fun Server.downloadMedia(mhUrl: MHUrl): Result<ByteArray, KomaFailure> {
    val req = when (mhUrl) {
        is MHUrl.Mxc -> {
            val u = mhUrl.toHttpUrl(this.url)?:return Result.failure(OtherFailure("url $mhUrl"))
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
    class Mxc(val mxc: String): MHUrl()
    class Http(val http: HttpUrl,
               val maxStale: Int? = null): MHUrl()

    fun toHttpUrl(server: HttpUrl): HttpUrl? {
        return when (this) {
            is Http -> this.http
            is Mxc -> mxcToHttp(this.mxc, server)
        }
    }

    override fun toString(): String {
        return when (this) {
            is Http -> this.http.toString()
            is Mxc -> this.mxc
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return (this is Mxc && other is Mxc && this.mxc == other.mxc)
        || (this is Http && other is Http && this.http == other.http && this.maxStale == other.maxStale)
    }

    override fun hashCode(): Int {
        return when (this ) {
            is Mxc -> this.mxc.hashCode()
            is Http -> this.http.hashCode() * 31 + (this.maxStale ?: 0)
        }
    }



    companion object {
        fun fromStr(url: String): Result<MHUrl, Exception>{
            if (url.startsWith("mxc://")) {
                return Result.of(Mxc(url))
            } else {
                val h = HttpUrl.parse(url)
                h ?: return Result.error(NullPointerException("Unknown URL $url"))
                return Result.of(Http(h))
            }
        }
    }
}

fun String.parseMxc(): MHUrl? {
    return MHUrl.fromStr(this).fold({it}, {null})
}