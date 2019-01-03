package koma.storage.config.server.cert_trust

import java.io.InputStream
import javax.net.ssl.SSLContext

fun sslConfFromStream(certStream: InputStream): Pair<SSLContext, CompositeX509TrustManager> {
    val ks = createKeyStore(certStream)
    val tm = CompositeX509TrustManager(ks)
    val sc = SSLContext.getInstance("TLS")
    sc.init(null, arrayOf(tm), null)
    return  Pair(sc, tm)
}
