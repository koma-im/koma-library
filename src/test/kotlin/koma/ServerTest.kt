package koma

import io.ktor.client.call.NoTransformationFoundException
import koma.matrix.UserId
import koma.network.client.okhttp.KHttpClient
import koma.util.failureOrThrow
import koma.util.getOrThrow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonDecodingException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.EOFException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

internal class ServerTest {

    @Test
    fun getDisplayNameKtor() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)

        val user1 = "@joseph"
        val displayname = "Joseph"
        val r1 = MockResponse().setBody("""{"displayname": "$displayname"} """)
        r1.headers =  r1.headers.newBuilder().set("Content-Type", "application/json").build()
        server.enqueue(r1)
        val n1= runBlocking {
            s.getDisplayNameKtor(UserId(user1))
        }
        assert(n1.isSuccess)
        val d = n1.getOrThrow()
        assertEquals(displayname, d.displayname)
        val req1 = server.takeRequest()
        val url1 = req1.requestUrl!!
        val p1 = url1.pathSegments
        assertEquals(listOf("profile", user1, "displayname"), p1.drop(4))

        server.enqueue(MockResponse().apply {
            headers = headers.newBuilder().set("Content-Type", "text/html").build()
        })
        val user = "us"
        val n= runBlocking {
            s.getDisplayNameKtor(UserId(user))
        }
        val f = n.failureOrThrow()
        assertTrue(f is InvalidData)
        val exception = (f as InvalidData).cause
        assert(exception is JsonDecodingException) { "unexpected exception $exception"}
        val req = server.takeRequest()
        val url = req.requestUrl!!
        val p = url.pathSegments
        assertEquals(listOf("profile", user, "displayname"), p.drop(4))

        server.enqueue(MockResponse())
        val user2 = "us2"
        val n2= runBlocking {
            s.getDisplayNameKtor(UserId(user2))
        }
        val f2 = n2.failureOrThrow()
        assertTrue(f2 is InvalidData) { "unexpected failure $f2"}
        val exception2 = (f2 as InvalidData).cause
        assert(exception2 is NoTransformationFoundException)
        val req2 = server.takeRequest()
        val url2 = req2.requestUrl!!
        val p2 = url2.pathSegments
        assertEquals(listOf("profile", user2, "displayname"), p2.drop(4))
    }

    @Test
    fun getDisplayNameKtorTimeout() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())

        val user1 = "@joseph"
        val displayname = "Joseph"
        val r1 = MockResponse().setBody("""{"displayname": "$displayname"} """).setHeadersDelay(100, TimeUnit.MILLISECONDS)
        r1.headers = r1.headers.newBuilder().set("Content-Type", "application/json").build()
        server.enqueue(r1)
        val n1 = runBlocking {
            s.getDisplayNameKtor(UserId(user1))
        }
        assert(n1.isFailure) {"Unexpected $n1"}
        val f = n1.failureOrThrow()
        assert(f is Timeout)
        assert((f as Timeout).cause is SocketTimeoutException)
        val req1 = server.takeRequest()
        val url1 = req1.requestUrl!!
        val p1 = url1.pathSegments
        assertEquals(listOf("profile", user1, "displayname"), p1.drop(4))
    }

    @Test
    fun getApiURL() {
    }
}