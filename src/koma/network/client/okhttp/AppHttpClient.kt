package koma.network.client.okhttp

import koma.Koma
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConf
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.net.Proxy

/**
 * try to always reuse this client instead of creating a new one
 */
class AppHttpClient(
        private val paths: ConfigPaths,
        private val koma: Koma,
        proxy: Proxy? = null
) {
    val client: OkHttpClient
    val builder: OkHttpClient.Builder

    fun builderForServer(serverConf: ServerConf): OkHttpClient.Builder {
        val addtrust = koma.servers.loadServerCert(serverConf)
        return if (addtrust != null) {
            builder.sslSocketFactory(addtrust.first.socketFactory, addtrust.second)
        } else {
            builder
        }
    }

    init {

        val conpoo = ConnectionPool()
        var b = OkHttpClient.Builder().connectionPool(conpoo)
        if (proxy != null) b = b.proxy(proxy)
        builder = b
        client = setUpClient()
    }

    private fun setUpClient(): OkHttpClient {
        return builder.tryAddAppCache("http", 80*1024*1024, koma.paths)
                .trySetAppCert(paths)
                .build()
    }
}
