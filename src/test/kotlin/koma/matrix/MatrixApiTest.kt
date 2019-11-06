package koma.matrix

import koma.IOFailure
import koma.Server
import koma.matrix.room.naming.RoomId
import koma.network.client.okhttp.KHttpClient
import koma.util.failureOrThrow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

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
    fun g1() {

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
}