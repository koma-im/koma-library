package koma.matrix.media

import com.github.kittinunf.result.Result
import okhttp3.HttpUrl

/**
 * convert matrix media url to normal HttpUrl
 * mediaApiPath is like _matrix/media/r0/, "download" needs to be added to get a resource
 */
fun parseMediaUrl(url: String, server: HttpUrl, mediaApiPath: String): Result<HttpUrl, Exception> {
    if (url.startsWith("mxc://")) {
        return Result.of(parseMxcUrl(url, server, mediaApiPath))
    } else {
        val h = HttpUrl.parse(url)
        h ?: return Result.error(NullPointerException("Unknown URL $url"))
        return Result.of(h)
    }
}

fun parseMxcUrl(addr: String, server: HttpUrl, mediaApiPath: String): HttpUrl {
    val parts = addr.substringAfter("mxc://")
    val serverName = parts.substringBefore('/')
    val mediaId = parts.substringAfter('/')

    val u = HttpUrl.Builder()
            .scheme(server.scheme())
            .host(server.host())
            .port(server.port())
            .addPathSegments(mediaApiPath)
            .addPathSegment("download")
            .addPathSegment(serverName)
            .addPathSegment(mediaId)
            .build()
    return u
}
