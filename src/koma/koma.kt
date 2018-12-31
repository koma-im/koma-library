package koma

import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConfStore
import java.net.Proxy

class Koma(val paths: ConfigPaths, proxy: Proxy) {
    val http = AppHttpClient(paths, this, proxy)
    val servers = ServerConfStore(paths)
}
