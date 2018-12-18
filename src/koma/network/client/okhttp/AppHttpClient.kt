package koma.network.client.okhttp

import koma.Koma
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConf
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

/**
 * try to always reuse this client instead of creating a new one
 */
class AppHttpClient(private val paths: ConfigPaths, private val koma: Koma) {
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
        val proxy = koma.appSettings.getProxy()
        val conpoo = ConnectionPool()
        builder = OkHttpClient.Builder()
                .proxy(proxy)
                .connectionPool(conpoo)
        client = setUpClient()
    }

    private fun setUpClient(): OkHttpClient {
        return builder.tryAddAppCache("http", 80*1024*1024, koma.paths)
                .trySetAppCert(paths)
                .build()
    }
}
