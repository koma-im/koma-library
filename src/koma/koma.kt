package koma

import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConfStore
import koma.storage.config.settings.AppSettings

class Koma(data_dir: String) {
    val paths = ConfigPaths(data_dir)
    val appSettings = AppSettings(paths)
    val http = AppHttpClient(paths, this)
    val servers = ServerConfStore(paths)
}
