package koma.matrix

import koma.Server
import koma.Timeout
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.json.jsonDefault
import koma.matrix.room.naming.RoomId
import koma.matrix.sync.Events
import koma.matrix.sync.RoomsResponse
import koma.matrix.sync.SyncResponse
import koma.network.client.okhttp.KHttpClient
import koma.util.failureOrThrow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
internal class ApiCallsKtTest {
    private val client  = KHttpClient.client
    @Test
    fun loginTest() {
        val s = Server("http://server".toHttpUrlOrNull()!!, client)
        runBlocking {
            s.login(UserPassword(user = "u", password = "p"))
        }
    }
    @Test
    fun apiTest() {
        val s = Server("http://server".toHttpUrlOrNull()!!, client)
        val a = s.account(UserId("uid"), "token")
        runBlocking {
            a.sendMessage(RoomId("room"), TextMessage("msg"))
        }
    }

    @Test
    fun testMock() {
        val server = MockWebServer()
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val res= MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(jsonDefault.stringify(SyncResponse.serializer(), sync))
        repeat(4) { server.enqueue(res) }
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, client)
        val a = s.account(UserId("uid"), "token")
        val r = runBlocking { a.sync("test") }
        assert(r.isSuccess) { "Expected success ${r.failureOrNull()}"}
        val r1 = runBlocking { a.sync("test1") }
        assert(r1.isSuccess)
        val r2 = runBlocking { a.sync("test2", timeout =1000.milliseconds) }
        assert(r2.isSuccess)
        val r3 = runBlocking { a.sync("test3", timeout =1000.milliseconds) }
        assert(r3.isSuccess)

        repeat(2) {  server.enqueue(res) }
        assert(runBlocking { a.sync("test3.5", timeout =1000.milliseconds) }.isSuccess)
        assert(runBlocking { a.sync("test3.4", timeout =1000.milliseconds) }.isSuccess)

        repeat(2) { server.enqueue(res.clone().setHeadersDelay(101, TimeUnit.MILLISECONDS)) }
        val r40 = runBlocking {
            a.sync("test4-0", timeout =1.milliseconds, networkTimeout = 100.milliseconds)
        }
        assert(r40.isFailure)
        assert(r40.failureOrThrow() is Timeout)

        val r4 = runBlocking { a.sync("test4", timeout =1.milliseconds, networkTimeout = 100.milliseconds) }
        assert(r4.isFailure)
        val f = r4.failureOrThrow()
        assert(f is Timeout)

        server.enqueue(res)
        assert(runBlocking { a.sync("test5", timeout =1.milliseconds, networkTimeout = 100.milliseconds) }.isSuccess)

        server.shutdown()
    }

    @Test
    fun testAfterExecuteTimeout() {
        val server = MockWebServer()
        val disp = TestingDispatcher()
        server.dispatcher = disp
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val res= MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(jsonDefault.stringify(SyncResponse.serializer(),sync))
        server.start()
        val base = server.url("mock")
        val s = Server(base, client)
        val a = s.account(UserId("uid"), "token")

        assertEquals(0, disp.queuedCount())
        assertEquals(0, server.requestCount)
        server.enqueue(res)
        assertEquals(1, disp.queuedCount())
        assertEquals(0, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        val response =runBlocking {
            a.sync("00", timeout =10.milliseconds, networkTimeout = 350.milliseconds)
        }
        assert ( response.isSuccess ) { "response $response"}
        assertEquals(0, disp.queuedCount())
        assertEquals(1, server.requestCount)
        server.takeRequest()

        server.enqueue(res.clone().setHeadersDelay(101, TimeUnit.MILLISECONDS))
        assertEquals(1, disp.queuedCount())
        assertEquals(101, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        val (success, failure, result) = runBlocking {
             a.sync("01", networkTimeout = 1.milliseconds)
        }
        assert(result.isFailure)
        assert(success == null)
        assert(failure != null)
        assert(failure is Timeout)
        assertEquals(2, server.requestCount)
        assertEquals(0, disp.queuedCount())
        server.takeRequest()

        server.enqueue(res)
        assertEquals(1, disp.queuedCount())
        assertEquals(res.getHeadersDelay(TimeUnit.MILLISECONDS), server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        assertEquals(0, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
        val r2 = runBlocking { a.sync("02", networkTimeout = 100.milliseconds) }
        assert(r2.isSuccess)
        assertEquals(3, server.requestCount)
        assertEquals(0, disp.queuedCount())
        server.takeRequest()

        server.enqueue(res)
        assertEquals(0, server.dispatcher.peek().getHeadersDelay(TimeUnit.MILLISECONDS))
    }

    @Test
    fun testAfterAsyncTimeout() {
        val server = MockWebServer()
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val res = MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(jsonDefault.stringify(SyncResponse.serializer(),sync))
        server.enqueue(res.clone().setHeadersDelay(101, TimeUnit.MILLISECONDS))
        server.start()
        val base = server.url("mock")
        val s = Server(base, client)
        val a = s.account(UserId("uid"), "token")
        //val p = a.getEventPoller(10, 100)
        val r4 = runBlocking { a.sync("01", timeout = 10.milliseconds, networkTimeout = 100.milliseconds) }
        assert(r4.isFailure)
        val f = r4.failureOrThrow()
        assert(f is Timeout)

        server.enqueue(res)
        val r3 = runBlocking {
             a.sync("02", timeout = 10.milliseconds, networkTimeout = 100.milliseconds)
        }
        assert(r3.isSuccess) { "$r3  is not Success "}
        server.enqueue(res)
        assertTrue {
            runBlocking {
                val r = a.sync("02", timeout = 10.milliseconds, networkTimeout = 100.milliseconds)
                r.isSuccess
            }
        }
        server.shutdown()
    }
}

private class TestingDispatcher: QueueDispatcher() {
    fun queuedCount(): Int {
        return responseQueue.size
    }
}