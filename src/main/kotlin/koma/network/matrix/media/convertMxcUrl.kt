package koma.network.matrix.media

import okhttp3.HttpUrl

/**
 * convert mxc:// to https://
 */
fun mxcToHttp(mxc: String, serverUrl: HttpUrl, mediaPath: String = "_matrix/media/r0/download"): HttpUrl? {
    val parts = mxc.substringAfter("mxc://")
    val serverName = parts.substringBefore('/')
    val media = parts.substringAfter('/')

    val url =try {
        serverUrl.newBuilder()
                .addPathSegments(mediaPath)
                .addPathSegment(serverName)
                .addPathSegment(media)
                .build()
    } catch (e: NullPointerException) {
        System.err.println("failed to convert $mxc using $serverUrl")
        e.printStackTrace()
        return null
    }
    return url
}
