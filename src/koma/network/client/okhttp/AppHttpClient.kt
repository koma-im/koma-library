package koma.network.client.okhttp

import koma.storage.config.server.cert_trust.sslConfFromStream
import mu.KotlinLogging
import okhttp3.*
import java.io.File
import java.io.InputStream
import java.net.Proxy

private val logger = KotlinLogging.logger {}

/**
 * 80 megabytes
 */
private const val cacheSize: Long = 80*1024*1024

/**
 * try to always reuse this client instead of creating a new one
 */
class AppHttpClient(
        /**
         * can be used to trust mitmproxy, useful for debugging
         */
        trustAdditionalCertificate: InputStream? = null,
        /**
         * enable http cache using the directory on disk
         */
        cacheDir: File? = null,
        /**
         * provide a builder to configure the http client
         * some options are still overridden
         * such as connection pool, proxy
         */
        http_builder: OkHttpClient.Builder? = null,
        proxy: Proxy? = null
) {
    val client: OkHttpClient
    val builder: OkHttpClient.Builder

    init {

        val conpoo = ConnectionPool()
        var b = http_builder?: OkHttpClient.Builder()
        b = b.connectionPool(conpoo)
        if (proxy != null) b = b.proxy(proxy)
        if (cacheDir != null) {
            b = b.cache(Cache(cacheDir, cacheSize))
        }
        if (trustAdditionalCertificate != null) {
            val (s, m) = sslConfFromStream(trustAdditionalCertificate)
            b = b.sslSocketFactory(s.socketFactory, m)
        }
        builder = b.addInterceptor(RetryGetPeerCert())
        client = builder.build()
        client.dispatcher()
    }
}

/**
 * sometimes there are IndexOutOfBoundsExceptions on Okhttp Dispatcher thread
 * the cause may be that get is called on an empty list
 */
class RetryGetPeerCert: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        for (i in 1..3) {
            try {
                val res = chain.proceed(req)
                return res
            } catch (e: IndexOutOfBoundsException) {
                logger.error { "Request ${req.url()} got $e, retry $i" }
                continue
            }

        }
        logger.error { "Request ${req.url()} aborted" }
        val res = Response.Builder()
                .code(404)
                .build()
        return res
    }
}
