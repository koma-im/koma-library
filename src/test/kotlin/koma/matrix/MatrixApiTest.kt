package koma.matrix

import koma.Failure
import koma.IOFailure
import koma.OtherFailure
import koma.Server
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.json.jsonDefault
import koma.matrix.pagination.FetchDirection
import koma.matrix.publicapi.rooms.RoomDirectoryFilter
import koma.matrix.publicapi.rooms.RoomDirectoryQuery
import koma.matrix.room.naming.RoomId
import koma.matrix.user.AvatarUrl
import koma.network.client.okhttp.KHttpClient
import koma.network.media.MHUrl
import koma.util.KResult
import koma.util.failureOrThrow
import koma.util.getOr
import koma.util.getOrThrow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.EOFException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalContracts
internal class MatrixApiTest {

    @Test
    fun getRoomName() {
        val server = MockWebServer()
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        runBlocking {
            val n= api.getRoomName(RoomId("r"))
            assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is IOFailure)
            val i = (f as IOFailure)
            assert(i.throwable is SocketTimeoutException)
        }
    }

    @Test
    fun setUserAvatar() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)
        server.enqueue(MockResponse())
        val api = s.account(UserId("u"), "token")
        val uid = "@Joe"
        runBlocking {
            val n= api.updateAvatar(UserId(uid), avatarUrl = AvatarUrl("avurl"))
            assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is IOFailure) { "fail $f"}
            val i = (f as IOFailure)
            assert(i.throwable is EOFException) //empty response
        }
        val req = server.takeRequest()
        val p = req.path!!
        assert(p.contains(uid))

        val url = req.requestUrl!!
        val u = url.pathSegments[5]
        assertEquals(uid, u)
    }

    @Test
    fun testGetRoomMessage() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)
        server.enqueue(MockResponse())
        val api = s.account(UserId("u"), "token")
        val roomId = "room8"
        val from = "aaaaaaaaaaa"
        val n= runBlocking {
            api.getRoomMessages(RoomId(roomId), from, FetchDirection.Backward)
        }
        assert(n.isFailure)
        val f = n.failureOrThrow()
        assertTrue(f is IOFailure) { "fail $f"}
        val i = (f as IOFailure)
        assert(i.throwable is EOFException) //empty response
        val req = server.takeRequest()
        val url = req.requestUrl!!
        assertEquals(roomId, url.pathSegments[5])
        assertEquals("token", url.queryParameter("access_token"))
        assertEquals(from, url.queryParameter("from"))
        assertEquals("b", url.queryParameter("dir"))
        assertEquals("100", url.queryParameter("limit"))
    }

    @Test
    fun testGetPubs() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)
        server.enqueue(MockResponse())
        val api = s.account(UserId("u"), "token")
        val term = "kotl"
        val query = RoomDirectoryQuery(RoomDirectoryFilter(term))
        val n= runBlocking {
            api.findPublicRooms(query)
        }
        val f = n.failureOrThrow()
        assertTrue(f is IOFailure) { "fail $f"}
        val i = (f as IOFailure)
        assert(i.throwable is EOFException) //empty response
        val req = server.takeRequest()
        val url: HttpUrl = api.server.apiURL
        assertEquals(url.encodedPath+"publicRooms", req.requestUrl!!.encodedPath)
        val body = req.body.readUtf8()
        val q1 = jsonDefault.parse(RoomDirectoryQuery.serializer(), body)
        assertEquals(20, q1.limit)
        assertEquals(term, q1.filter.generic_search_term)
    }

    @Test
    fun testSendMessage() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)
        server.enqueue(MockResponse())
        val api = s.account(UserId("u"), "token")
        val roomId = "room8"
        val msg = "empty"
        val n= runBlocking {
            api.sendMessage(RoomId(roomId), TextMessage(msg))
        }
        val f = n.failureOrThrow()
        assertTrue(f is IOFailure) { "fail $f"}
        val i = (f as IOFailure)
        assert(i.throwable is EOFException) //empty response
        val req = server.takeRequest()
        val url = req.requestUrl!!
        val p = url.pathSegments
        assertEquals(roomId, p[5])
        assertEquals("m.room.message", p[7])
        val body = req.body.readUtf8()
        assertEquals("{\"body\":\"$msg\",\"msgtype\":\"m.text\"}", body)
    }

    private suspend fun returnNonLocal(): KResult<Unit, Failure> {
        val server = MockWebServer()
        @Suppress("BlockingMethodInNonBlockingContext")
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        val nq = api.getRoomName(RoomId("r")) getOrUnstable {
            return KResult.failure(OtherFailure("some failure"))
        }
        error("unexpected success")
    }

    @Test
    fun testInlineCastError() {
        assertDoesNotThrow {
            runBlocking {
                returnNonLocal()
            }
        }
        assertThrows<ClassCastException> {
            val result = runBlocking {
                returnNonLocal()
            }
        }
    }

    @Test
    fun getRoomAvatar() {
        val server = MockWebServer()
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        runBlocking {
            val n = api.getRoomAvatar(RoomId("r"))
              assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is IOFailure)
            val i = (f as IOFailure)
            assert(i.throwable is SocketTimeoutException)
        }
    }

    @Test
    fun updateAvatar() {
    }
}

@ExperimentalContracts
@Suppress("NOTHING_TO_INLINE")
private inline infix fun <R, T : R, E: Any> KResult<T, E>.getOrUnstable( onFailure: (E) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return if (isFailure) {
        onFailure(failureOrThrow())
    } else {
        getOrThrow()
    }
}
