package koma

import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.getHttpCacheDir
import koma.util.KResult
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

    private val downloader = Downloader(http.client)

    suspend fun downloadMedia(url: HttpUrl, maxStale: Int? = null): KResult<ByteArray, KomaFailure> {
        return downloader.downloadMedia(url, maxStale)
    }
    /**
     * instance of a matrix server
     */
    fun server(server: HttpUrl): Server {
        return Server(server, this)
    }
}

