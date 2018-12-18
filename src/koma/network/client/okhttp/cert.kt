package koma.network.client.okhttp

import koma.storage.config.ConfigPaths
import koma.storage.config.server.cert_trust.loadContext
import okhttp3.OkHttpClient

fun OkHttpClient.Builder.trySetAppCert(paths: ConfigPaths): OkHttpClient.Builder {
    val certdir = paths.getCreateDir("settings") ?: return this
    val ctx = loadContext(certdir) ?: return this
    return  this.sslSocketFactory(ctx.first.socketFactory, ctx.second)
}
