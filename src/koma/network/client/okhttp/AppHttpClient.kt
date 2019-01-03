package koma.network.client.okhttp

import koma.Koma
import koma.storage.config.server.cert_trust.sslConfFromStream
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.InputStream
import java.net.Proxy

/**
 * try to always reuse this client instead of creating a new one
 */
class AppHttpClient(
        private val koma: Koma,
        /**
         * can be used to trust mitmproxy, useful for debugging
         */
        trustAdditionalCertificate: InputStream? = null,
        proxy: Proxy? = null
) {
    val client: OkHttpClient
    val builder: OkHttpClient.Builder

    init {

        val conpoo = ConnectionPool()
        var b = OkHttpClient.Builder().connectionPool(conpoo)
        if (proxy != null) b = b.proxy(proxy)
        b = b.tryAddAppCache("http", 80*1024*1024, koma.paths)
        if (trustAdditionalCertificate != null) {
            val (s, m) = sslConfFromStream(trustAdditionalCertificate)
            b = b.sslSocketFactory(s.socketFactory, m)
        }
        builder = b
        client = builder.build()
    }
}
