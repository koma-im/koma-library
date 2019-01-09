package koma

import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConfStore
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.Proxy

private val logger = KotlinLogging.logger {}

class Koma(
        val paths: ConfigPaths,
        proxy: Proxy,
        /**
         * Provide a configured builder to configure some options of the http client
         */
        http_builder: OkHttpClient.Builder? = null,
        /**
         * additional certificate to trust
         */
        addTrust: InputStream? = null
) {
    val http = AppHttpClient(
            cacheDir = paths.getCreateDir("data", "cache", "http"),
            trustAdditionalCertificate = addTrust ?: loadOptionalCert(paths),
            http_builder = http_builder,
            proxy = proxy)
    val servers = ServerConfStore(paths)

    fun createApi(token: String, userId: UserId, server: HttpUrl): MatrixApi {
        return MatrixApi(token, userId, server, http = http)
    }
}

/**
 * if there is a certificate file at the given path
 * it will be trusted
 */
private fun loadOptionalCert(paths: ConfigPaths): InputStream? {
    val f = paths.getCreateDir("settings")?.resolve("self-cert.crt")
    f ?: return null
    try {
        return  f.inputStream()
    } catch (e: FileNotFoundException) {
    } catch (e: Exception) {
        logger.warn { "Loaded no certificate from ${f.path}: $e" }
    }
    return null
}
