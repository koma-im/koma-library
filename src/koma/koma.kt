package koma

import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConfStore
import mu.KotlinLogging
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.Proxy

private val logger = KotlinLogging.logger {}

class Koma(val paths: ConfigPaths, proxy: Proxy) {
    val http = AppHttpClient(this,
            trustAdditionalCertificate = loadOptionalCert(paths),
            proxy = proxy)
    val servers = ServerConfStore(paths)
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
