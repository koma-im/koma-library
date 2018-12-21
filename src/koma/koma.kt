package koma

import koma.koma_app.SaveJobs
import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.ConfigPaths
import koma.storage.config.server.ServerConfStore

class Koma(data_dir: String) {
    val paths = ConfigPaths(data_dir)
    val http = AppHttpClient(paths, this)
    val servers = ServerConfStore(paths)

    /**
     * Call after use to save access_token to disk
     */
    fun saveToDisk() {
        SaveJobs.finishUp()
    }
}
