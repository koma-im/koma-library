package koma.controller.sync

import com.squareup.moshi.JsonEncodingException
import koma.IOFailure
import koma.InvalidData
import koma.Server
import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.matrix.json.jsonDefault
import koma.matrix.sync.Events
import koma.matrix.sync.RoomsResponse
import koma.matrix.sync.SyncResponse
import koma.network.client.okhttp.KHttpClient.client
import koma.util.failureOrThrow
import koma.util.getOrThrow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonDecodingException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.IOError
import kotlin.time.milliseconds

internal class MatrixSyncReceiverTest {

    @Test
    fun startSyncing() {
        val server = MockWebServer()
        val sync = SyncResponse("next_bat", Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf()))
        val b = jsonDefault.stringify(SyncResponse.serializer(), sync)
        val res= MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(b)
        server.enqueue(res)
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, client)
        val a = s.account(UserId("uid"), "token")
        val m = MatrixSyncReceiver(a, "since0")
        GlobalScope.launch {
            m.startSyncing()
        }
        val req0 = server.takeRequest()
        val url0 = req0.requestUrl!!
        assertEquals("token", url0.queryParameter("access_token"))
        assertEquals("since0", url0.queryParameter("since"))
        assertEquals("50000", url0.queryParameter("timeout"))
        val res0 = runBlocking { m.events.receive() }.getOrThrow()
        assertEquals(sync, res0)

        val req1 = server.takeRequest()
        val url1 = req1.requestUrl!!
        assertEquals("token", url1.queryParameter("access_token"))
        assertEquals("next_bat", url1.queryParameter("since"))
        assertEquals("50000", url1.queryParameter("timeout"))
        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("not-json"))
        val res1 = runBlocking { m.events.receive() }.failureOrThrow()
        assert(res1 is InvalidData) { "res $res1"}
        res1 as InvalidData
        assert(res1.cause is JsonDecodingException) { "unexpected failure $res1"}

        val req2 = server.takeRequest()
        val url2 = req2.requestUrl!!
        assertEquals("token", url2.queryParameter("access_token"))
        assertEquals("next_bat", url2.queryParameter("since"))
        assertEquals("1000", url2.queryParameter("timeout"))
        
        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(jsonDefault.stringify(SyncResponse.serializer(), sync.copy(next_batch = "nb3"))))
        val res3 = runBlocking { m.events.receive() }.getOrThrow()
        assertEquals("nb3", res3.next_batch)

        val req3 = server.takeRequest()
        val url3 = req3.requestUrl!!
        assertEquals("token", url3.queryParameter("access_token"))
        assertEquals("nb3", url3.queryParameter("since"))
        assertEquals("50000", url3.queryParameter("timeout"))

    }
}