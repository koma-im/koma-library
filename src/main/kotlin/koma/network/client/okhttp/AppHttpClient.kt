package koma.network.client.okhttp

import koma.util.given
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * 80 megabytes
 */
private const val cacheSize: Long = 80*1024*1024

/**
 * provides instance of OkHttpClient for resource sharing.
 * to change options, call newBuilder to create shallow copies.
 * this client contains workarounds for some issues.
 */
object KHttpClient {
    val client: OkHttpClient

    init {
        client = OkHttpClient.Builder()
                .addInterceptor(RetryGetPeerCert())
                .eventListenerFactory(object : EventListener.Factory {
                    val callId = AtomicInteger(0)
                    override fun create(call: okhttp3.Call): EventListener {
                        return CloseTimeoutSocketListener(callId.getAndIncrement())
                    }
                })
                .dispatcher(Dispatcher().apply {
                    this.maxRequestsPerHost = 10
                })
                .build()
    }
}

/**
 * sometimes there are IndexOutOfBoundsExceptions on Okhttp Dispatcher thread
 * the cause may be that get is called on an empty list
 */
private class RetryGetPeerCert: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        for (i in 1..3) {
            try {
                val res = chain.proceed(req)
                return res
            } catch (e: IndexOutOfBoundsException) {
                logger.error { "Request ${req.url} got $e, retry $i" }
                continue
            }

        }
        logger.error { "Request ${req.url} aborted" }
        val res = Response.Builder()
                .code(404)
                .build()
        return res
    }
}



internal class CloseTimeoutSocketListener(
        private val id: Int
): EventListener() {
    var isNewConn = false
    private var connection: Connection? = null

    override fun connectStart(call: okhttp3.Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        isNewConn = true
        logger.debug {
            val u = call.request().url
            "$id is new conn to host=${u.host} path=${u.encodedPath} addr=$inetSocketAddress via $proxy"
        }
    }

    override fun connectionAcquired(call: okhttp3.Call, connection: Connection) {
        this.connection = connection
    }

    override fun callFailed(call: okhttp3.Call, ioe: IOException) {
        if (!isNewConn && ioe is SocketTimeoutException) {
            connection?.run {
                logger.debug { "call $id to ${call.request().url} in pool timed out, closing socket" }
                val s = this.socket()
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        s.close()
                    } catch (e: Exception) {
                        logger.error { "error closing timeout socket: $e" }
                    }
                }

            }?: logger.error { "connection unknown" }
        } else {
            logger.debug {
                val u = call.request().url
                "call $id host=${u.host} path=${u.encodedPath} fail $ioe"
            }
        }
    }
}