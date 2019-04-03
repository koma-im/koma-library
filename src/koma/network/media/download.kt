package koma.network.media

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import koma.Koma
import koma.network.matrix.media.mxcToHttp
import koma.util.coroutine.adapter.okhttp.await
import koma.util.coroutine.adapter.okhttp.extract
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit

suspend fun Koma.getResponse(url: HttpUrl): Result<ResponseBody, Exception> {
    val req = Request.Builder().url(url).build()
    val httpres = this.http.client.newCall(req).await()
    return httpres.flatMap { res -> res.extract() }
}

suspend fun Koma.downloadMedia(mhUrl: MHUrl, server: HttpUrl): Result<ByteArray, Exception> {
    val req = when (mhUrl) {
        is MHUrl.Mxc -> {
            val h = mhUrl.toHttpUrl(server)
            if (h is Result.Failure) return Result.error(h.error)
            val u = h.get()
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
    val bs = getHttpBytes(req)
    return bs
}

private suspend fun Koma.getHttpBytes(req: Request): Result<ByteArray, Exception> {
    val hr = this.http.client.newCall(req).await()
    return hr.flatMap { it.extract() }.map { it.bytes() }
}

/**
 * matrix or http media url
 */
sealed class MHUrl {
    class Mxc(val mxc: String): MHUrl()
    class Http(val http: HttpUrl,
               val maxStale: Int? = null): MHUrl()

    fun toHttpUrl(server: HttpUrl): Result<HttpUrl, Exception> {
        return when (this) {
            is Http -> Result.of(this.http)
            is Mxc -> {
                mxcToHttp(this.mxc, server)
                        ?.let { Result.of(it) }
                        ?: Result.error(NullPointerException("Matrix media server not set"))
            }
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
