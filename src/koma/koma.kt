package koma

import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.getHttpCacheDir
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.InputStream
import java.net.Proxy

private val logger = KotlinLogging.logger {}

class Koma(
        proxy: Proxy,
        path: String? = System.getProperty("java.io.tmpdir"),
        /**
         * Provide a configured builder to configure some options of the http client
         */
        http_builder: OkHttpClient.Builder? = null,
        /**
         * additional certificate to trust
         */
        addTrust: InputStream? = null
) {
    val http: AppHttpClient
    init {
        val cache = path?.let { getHttpCacheDir(it) }
        logger.debug { "http cache directory $cache" }
        http = AppHttpClient(
                cacheDir = cache,
                trustAdditionalCertificate = addTrust,
                http_builder = http_builder,
                proxy = proxy)
    }

    fun createApi(token: String, userId: UserId, server: HttpUrl): MatrixApi {
        return MatrixApi(token, userId, server, http = http)
    }
}

