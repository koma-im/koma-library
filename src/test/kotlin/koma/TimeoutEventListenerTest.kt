package koma

import mu.KotlinLogging
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

internal class TimeoutEventListenerTest {
    
    @Test
    fun testTimeout() {
        val server = MockWebServer()
        server.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
        val listeners = mutableListOf<CloseTimeoutSocketListener>()
        val client = OkHttpClient.Builder().eventListenerFactory(object : EventListener.Factory {
            override fun create(call: Call): EventListener {
                val l = CloseTimeoutSocketListener(listeners.size.toLong())
                listeners.add(l)
                return l
            }

        }).protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                .build()
        server.start()
        val base = server.url("mock/timeout")
        server.enqueue(MockResponse().setBody("mockEventTestBody0"))
        val req = Request.Builder().url(base).build()
        val res0 = client.newCall(req).execute()
        assertEquals("mockEventTestBody0", res0.body?.string())
        val e0 = listeners[0]
        assert(e0.isNewConn)
        server.enqueue(MockResponse().setBody("mockEventTestBody1"))
        val res1 = client.newCall(req).execute()
        assertEquals("mockEventTestBody1", res1.body?.string())
        val e1 = listeners[1]
        assertFalse(e1.isNewConn)

        server.enqueue(MockResponse().setHeadersDelay(30, TimeUnit.MILLISECONDS))
        val cShort = client.newBuilder().readTimeout(20, TimeUnit.MILLISECONDS).build()
        assertThrows<SocketTimeoutException> { cShort.newCall(req).execute() }
        val e2 = listeners[2]
        assertFalse(e2.isNewConn)

        server.enqueue(MockResponse().setBody("mockEventTestBody3"))
        val res3 = client.newCall(req).execute()
        assertEquals("mockEventTestBody3", res3.body?.string())
        val e3 = listeners[3]
        assert(e3.isNewConn)

        server.enqueue(MockResponse().setBody("mockEventTestBody4"))
        val res4 = client.newCall(req).execute()
        assertEquals("mockEventTestBody4", res4.body?.string())
        val e4 = listeners[4]
        assertFalse(e4.isNewConn)
    }
    @Test
    fun testMockEvent() {
        val disp = TestingDispatcher()
        val server = MockWebServer()
        server.dispatcher = disp
        server.protocols = listOf(Protocol.H2_PRIOR_KNOWLEDGE)
        val listeners = mutableListOf<TestingEventListener>()
        val client = OkHttpClient.Builder().eventListenerFactory(object : EventListener.Factory {
            override fun create(call: Call): EventListener {
                val l = TestingEventListener(listeners.size.toLong())
                listeners.add(l)
                return l
            }

        }).protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                .build()
        server.start()
        val base = server.url("v2/mock")
        server.enqueue(MockResponse().setBody("mockEventTestBody0"))
        assertEquals(1, disp.queuedCount())
        val req = Request.Builder().url(base).build()
        val res1 = client.newCall(req).execute()
        assertEquals("mockEventTestBody0", res1.body?.string())
        val e0 = listeners[0]
        assert(e0.callStart)
        assert(e0.isNewConn)
        assert(e0.connRelease)
        assert(e0.callEnd)
        assertFalse(e0.connFail)

        val c1 = client.newBuilder().readTimeout(10, TimeUnit.MILLISECONDS).build()
        repeat(2) {
            server.enqueue(MockResponse()
                    .setHeadersDelay(20, TimeUnit.MILLISECONDS)
                    .setBody("mockEventTestBody1"))
        }
        assertEquals(2, disp.queuedCount())
        assertThrows<SocketTimeoutException> {  c1.newCall(req).execute() }
        if (disp.queuedCount() == 2) {
            logger.debug { "removing timed out response" }
            disp.popQueue()
        }
        assertEquals(1, disp.queuedCount())
        assertEquals(2, listeners.size)
        val e1a = listeners[1]
        assert(e1a.callStart)
        assertFalse(e1a.isNewConn)
        assert(e1a.connRelease)
        assertFalse(e1a.callEnd)
        assert(e1a.connFail)
        assert(e1a.exception is SocketTimeoutException)

        val r1 = client.newCall(req).execute()
        assertEquals("mockEventTestBody1", r1.body?.string())
        val e1 = listeners[0]
        assert(e1.callStart)
        assert(e1.isNewConn)
        assert(e1.connRelease)
        assert(e1.callEnd)
        assertFalse(e0.connFail)

        repeat(5) {
            server.enqueue(MockResponse()
                    .setHeadersDelay(20, TimeUnit.MILLISECONDS)
                    .setBody("mockEventTestBody$it-0"))
            assertThrows<SocketTimeoutException> {  c1.newCall(req).execute() }
            val c2 = c1.newBuilder().readTimeout(50, TimeUnit.MILLISECONDS).build()
            server.enqueue(MockResponse().setBody("mockEventTestBody$it-1"))
            val re1 = c2.newCall(req).execute()
            assertEquals("mockEventTestBody$it-1", re1.body?.string())
            assertFalse(listeners.last().isNewConn)
            repeat(5) { i ->
                server.enqueue(MockResponse().setBody("mockEventTestBody$it-2-$i"))
                val re2 = c2.newCall(req).execute()
                assertEquals("mockEventTestBody$it-2-$i", re2.body?.string())
                assertFalse(listeners.last().isNewConn)
            }
        }
    }
}

private class CloseTimeoutSocketListener(
         private val id: Long
): EventListener() {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    var isNewConn = false
    private var connection: Connection? = null
    
    override fun connectStart(call: okhttp3.Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        isNewConn = true
        log.debug("$id is new conn to ${call.request()} $inetSocketAddress via $proxy")
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        this.connection = connection
    }

    override fun callFailed(call: okhttp3.Call, ioe: IOException) {
        if (!isNewConn && ioe is SocketTimeoutException) {
            connection?.run {
                log.debug("call $id in pool timed out, closing socket")
                this.socket().close()
            }?: error("connection unknown")
        } else {
            log.debug("call $id fail ${call.request()} $ioe")
        }
    }
}
private class TestingEventListener(
        private val id: Long
): EventListener() {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    var callStart = false
    var isNewConn = false
    var connRelease = false
    var callEnd = false
    var connFail = false
    var exception: IOException? = null

    override fun callStart(call: okhttp3.Call) {
        callStart = true
        log.debug("call $id start ${call.request()}")
    }

    override fun connectStart(call: okhttp3.Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        isNewConn = true
        log.debug("$id is new conn to ${call.request()} $inetSocketAddress via $proxy")
    }

    override fun connectionReleased(call: okhttp3.Call, connection: Connection) {
        connRelease = true
        log.debug("conn $id release ${call.request()} $connection")
    }

    override fun callEnd(call: okhttp3.Call) {
        callEnd = true
        log.debug("call $id end ${call.request()}")
    }

    override fun callFailed(call: okhttp3.Call, ioe: IOException) {
        connFail = true
        exception = ioe
        log.debug("call $id fail ${call.request()} $ioe")
    }
}

private class TestingDispatcher: QueueDispatcher() {
    fun queuedCount(): Int {
        return responseQueue.size
    }
    fun clearQueue() {
        responseQueue.clear()
    }
    fun popQueue() : MockResponse {
        return   responseQueue.take()
    }
}