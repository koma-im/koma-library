package koma.matrix

import koma.IOFailure
import koma.Koma
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.json.MoshiInstance
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.Events
import koma.matrix.sync.RoomsResponse
import koma.matrix.sync.SyncResponse
import koma.network.client.okhttp.AppHttpClient
import koma.util.failureOrThrow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.io.IOError
import java.lang.IllegalArgumentException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

internal class ApiCallsKtTest {
    private val km = Koma(Proxy.NO_PROXY)
    private val client  = km.http
    @Test
    fun loginTest() {
        runBlocking {
            login(UserPassword(user = "u", password = "p"), "http://server", client)
        }
    }
    @Test
    fun apiTest() {
        val s = km.server("http://server".toHttpUrlOrNull()!!)
        val a = s.account(UserId("uid"), "token")
        runBlocking {
            a.sendMessage(RoomId("room"), TextMessage("msg"))
        }
        assertThrows(IllegalArgumentException::class.java) { a.EventPoller(10000, client.client) }
        assertThrows(IllegalArgumentException::class.java) { a.getEventPoller(10000, 10000) }
        val p = a.getEventPoller(10000)
        assertEquals(10000, p.apiTimeout)
        assertEquals(20000, p.netTimeout)
        assertThrows(IllegalArgumentException::class.java) { p.withTimeout(10000, 10000) }
        val p1 = p.withTimeout(10001)
        assertNotSame(p1.client, p.client)
        assertEquals(10001, p1.apiTimeout)
        assertEquals(20001, p1.netTimeout)
        val p2 = p.withTimeout(10001, 20000)
        assertSame(p2.client, p.client)
        assertEquals(10001, p2.apiTimeout)
        assertEquals(20000, p2.netTimeout)
    }

    @Test
    fun testMock() {
        val server = MockWebServer()
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val adapter = MoshiInstance.moshi.adapter<SyncResponse>(SyncResponse::class.java)
        val res= MockResponse().setBody(adapter.toJson(sync))
        repeat(4) { server.enqueue(res) }
        server.start()
        val base = server.url("vx/mock")
        val s = km.server(base)
        val a = s.account(UserId("uid"), "token")
        val r = runBlocking { a.asyncEvents("test") }
        assert(r.isSuccess)
        val r1 = runBlocking { a.asyncEvents("test1") }
        assert(r1.isSuccess)
        val p = a.getEventPoller(1000)
        val r2 = runBlocking { p.getEvent("test2") }
        assert(r2.isSuccess)
        val r3 = runBlocking { p.getEvent("test3") }
        assert(r3.isSuccess)

        val p1 = p.withTimeout(1, 100)
        repeat(2) {  server.enqueue(res) }
        assert(runBlocking { p1.getEvent("test3.5") }.isSuccess)
        assert(runBlocking { p.getEvent("test3.4") }.isSuccess)

        repeat(2) { server.enqueue(res.clone().setHeadersDelay(101, TimeUnit.MILLISECONDS)) }
        assertThrows<SocketTimeoutException> {p1.getCall("test4-0").execute()  }

        val r4 = runBlocking { p1.getEvent("test4") }
        assert(r4.isFailure)
        val f = r4.failureOrThrow()
        assert(f is IOFailure && f.throwable is SocketTimeoutException)

        server.enqueue(res)
        assert(runBlocking { p1.getEvent("test5") }.isSuccess)

        server.shutdown()
    }

    @Test
    fun testAfterExecuteTimeout() {
        val server = MockWebServer()
        val disp = TestingDispatcher()
        server.dispatcher = disp
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val adapter = MoshiInstance.moshi.adapter<SyncResponse>(SyncResponse::class.java)
        val res= MockResponse().setBody(adapter.toJson(sync))
        server.start()
        val base = server.url("mock")
        val s = km.server(base)
        val a = s.account(UserId("uid"), "token")
        val p = a.getEventPoller(10, 100)

        assertEquals(0, disp.queuedCount())
        assertEquals(0, server.requestCount)
        server.enqueue(res)
        assertEquals(1, disp.queuedCount())
        assertEquals(0, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        assertDoesNotThrow { p.getCall("00").execute() }
        assertEquals(0, disp.queuedCount())
        assertEquals(1, server.requestCount)
        server.takeRequest()

        server.enqueue(res.clone().setHeadersDelay(101, TimeUnit.MILLISECONDS))
        assertEquals(1, disp.queuedCount())
        assertEquals(101, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        assertThrows<SocketTimeoutException> { p.getCall("01").execute() }
        assertEquals(2, server.requestCount)
        assertEquals(0, disp.queuedCount())
        server.takeRequest()

        server.enqueue(res)
        assertEquals(1, disp.queuedCount())
        assertEquals(res.getHeadersDelay(TimeUnit.MILLISECONDS), server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        assertEquals(0, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        assertDoesNotThrow { p.withTimeout(1,104).getCall("02").execute() }
        assertEquals(3, server.requestCount)
        assertEquals(0, disp.queuedCount())
        server.takeRequest()

        server.enqueue(res)
        assertEquals(0, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        assertDoesNotThrow { p.getCall("03").execute() }
        server.takeRequest()

        server.enqueue(res)
        assert(runBlocking { p.getEvent("03") }.isSuccess)
        server.takeRequest()

        server.shutdown()
    }

    @Test
    fun testAfterAsyncTimeout() {
        val server = MockWebServer()
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val adapter = MoshiInstance.moshi.adapter<SyncResponse>(SyncResponse::class.java)
        val res= MockResponse().setBody(adapter.toJson(sync))
        server.enqueue(res.clone().setHeadersDelay(101, TimeUnit.MILLISECONDS))
        server.start()
        val base = server.url("mock")
        val s = km.server(base)
        val a = s.account(UserId("uid"), "token")
        val p = a.getEventPoller(10, 100)
        val r4 = runBlocking { p.getEvent("01") }
        assert(r4.isFailure)
        val f = r4.failureOrThrow()
        assert(f is IOFailure && f.throwable is SocketTimeoutException)

        server.enqueue(res)
        assertDoesNotThrow { p.getCall("02").execute() }

        server.enqueue(res)
        assert(runBlocking { p.getEvent("02") }.isSuccess)

        server.shutdown()
    }
}

private class TestingDispatcher: QueueDispatcher() {
    fun queuedCount(): Int {
        return responseQueue.size
    }
}