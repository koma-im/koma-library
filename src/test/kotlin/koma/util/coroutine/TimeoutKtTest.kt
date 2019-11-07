package koma.util.coroutine

import koma.Server
import koma.Timeout
import koma.matrix.UserId
import koma.matrix.room.naming.RoomId
import koma.network.client.okhttp.KHttpClient
import koma.util.coroutine.adapter.okhttp.await
import koma.util.failureOrThrow
import koma.util.getOrThrow
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Suppress("UNUSED_VARIABLE")
@ExperimentalTime
internal class TimeoutKtTest {

    @Test
    fun withTimeoutTimeout() {
        runBlocking {
            var j: Job? = null
            val result =withTimeout(50.milliseconds) {
                j = launch {
                    delay(500)
                }
                delay(51)
            }
            assert(result.isFailure) { "not timeout"}
            assert(j!!.isCancelled) { "not cancelled" }
            val f: Timeout = result.failureOrThrow()
        }
    }
    @Test
    fun withTimeoutComplete() {
        runBlocking {
            var j: Job? = null
            val result = withTimeout(90.milliseconds) {
                j = async {
                    delay(1)
                }
            }
            assert(result.isSuccess)
            assertFalse(j!!.isCancelled)
            assert(j!!.isCompleted)
            val success: Unit = result.getOrThrow()
        }
    }

    @Test
    fun okhttp3Timeout() {
        val server = MockWebServer()
        server.enqueue(MockResponse())
        server.start()
        val base = server.url("vmock")
        val req = Request.Builder().url(base).build()
        val client = OkHttpClient.Builder().build()
        runBlocking {
            val waitResult = withTimeout(200.milliseconds) {
                client.newCall(req).await()
            }
            assert(waitResult.isSuccess)
            val callResult = waitResult.getOrThrow()
            assert(callResult.isSuccess)
        }
        runBlocking {
            val waitResult = withTimeout(200.milliseconds) {
                client.newCall(req).await()
            }
            assert(waitResult.isFailure)
            val f: Timeout = waitResult.failureOrThrow()
            assertEquals(200.milliseconds, f.duration)
        }
    }

    @Test
    fun getRoomAvatar() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)
        val api = s.account(UserId("u"), "token")
        runBlocking {
            val n = withTimeout(200.milliseconds) {
                api.getRoomAvatar(RoomId("r"))
            }
            assert(n.isFailure)
            val f: Timeout = n.failureOrThrow()
            assertEquals(200.milliseconds, f.duration)
        }
    }
}