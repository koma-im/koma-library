package koma.network.client.okhttp

import koma.storage.config.ConfigPaths
import okhttp3.Cache
import okhttp3.OkHttpClient

fun OkHttpClient.Builder.tryAddAppCache(name: String, size:Long, configPaths: ConfigPaths): OkHttpClient.Builder {
    val cachedir = configPaths.getCreateDir("data", "cache", name)
    cachedir ?: return this
    return this.cache(Cache(cachedir, size))
}
