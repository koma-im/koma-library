package koma.matrix

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.engine.okhttp.OkHttpEngine
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.util.InternalAPI
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import io.ktor.util.KtorExperimentalAPI
import koma.*
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.json.jsonDefault
import koma.matrix.pagination.FetchDirection
import koma.matrix.publicapi.rooms.RoomDirectoryFilter
import koma.matrix.publicapi.rooms.RoomDirectoryQuery
import koma.matrix.room.admin.CreateRoomSettings
import koma.matrix.room.naming.RoomId
import koma.matrix.room.visibility.RoomVisibility
import koma.matrix.user.AvatarUrl
import koma.network.client.okhttp.KHttpClient
import koma.util.KResult
import koma.util.failureOrThrow
import koma.util.getOrThrow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.JsonDecodingException
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@ExperimentalContracts
internal class MatrixApiTest {
    @InternalAPI
    @Test
    fun ktorHttpClient() {
        val ms = MockWebServer()
        val base = ms.url("vv")
        val s = Server(base, KHttpClient.client)
        val sok = s.okHttpClient as OkHttpClient
        assertEquals(10000,  sok.readTimeoutMillis)
        val api = s.account(UserId("u"), "token")
        val eng = api.longTimeoutClient.engine
        check(eng is OkHttpEngine)
        val field = OkHttpEngine::class.java.getDeclaredField("engine").apply {
            isAccessible = true
        }
        val ok = field.get(eng) as OkHttpClient
        assertEquals(100000, ok.readTimeoutMillis)
    }
    private val fastClient =  KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.SECONDS).build()
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
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json"))
        val api = s.account(UserId("u"), "token")
        val uid = "@Joe"
        runBlocking {
            val n= api.updateAvatar(UserId(uid), avatarUrl = AvatarUrl("avurl"))
            assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is InvalidData) { "fail $f"}
            val i = (f as InvalidData)
            assert(i.cause is JsonDecodingException)
        }
        val req = server.takeRequest()
        val p = req.path!!
        assert(p.contains(uid))
        assertEquals("PUT", req.method)

        val url = req.requestUrl!!
        val u = url.pathSegments[5]
        assertEquals(uid, u)
        assertEquals("avatar_url", url.pathSegments[6])
        assertEquals("token", url.queryParameter("access_token"))

        val body = req.body.readUtf8()
        assertEquals("""{"avatar_url":"avurl"}""", body)

        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{}""")
        )
        val response = runBlocking { api.updateAvatar(UserId(uid), avatarUrl = AvatarUrl("avurl")) }
        assert(response.isSuccess)
        val res: UpdateAvatarResult = response.getOrThrow()

        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""""")
        )
        val res2= runBlocking { api.updateAvatar(UserId(uid), avatarUrl = AvatarUrl("avurl")) }
        assert(res2.isFailure) { "fail $res2"}

        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{}""")
                .setResponseCode(403)
        )
        val response1 = runBlocking { api.updateAvatar(UserId(uid), avatarUrl = AvatarUrl("avurl")) }
        assert(response1.isFailure) { "fail $response1" }
        val f = response1.failureOrThrow()
        f as HttpFailure
        assertEquals(403, f.http_code)
        assertEquals("PUT", server.takeRequest().method)
    }

    @Test
    fun uploadMedia() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, fastClient)
        val api = s.account(UserId("u"), "token")
        val n= runBlocking {
             api.uploadMedia(ByteArrayContent(byteArrayOf(3,8,4), ContentType.Application.OctetStream))
        }
        assert(n.isFailure)
        val f0 = n.failureOrThrow()
        assertTrue(f0 is Timeout) { "fail $f0" }
        f0 as Timeout
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/mock/_matrix/media/r0/upload", req.path?.substringBefore('?'))

        val url = req.requestUrl!!
        assertEquals("token", url.queryParameter("access_token"))

        val body = req.body.readByteArray()
        assert(byteArrayOf(3, 8, 4).contentEquals(body))

        repeat(2) {
            server.enqueue(MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"content_uri": "url-of-uploaded-media"}"""))
        }
        val response = runBlocking { api.uploadMedia(ByteArrayContent(byteArrayOf(3,8,4), ContentType.Application.OctetStream)) }
        assert(response.isSuccess) { "fail $response"}
        val res = response.getOrThrow()
        assertEquals("url-of-uploaded-media", res.content_uri)
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
        assertTrue(f is InvalidData) { "fail $f"}
        val i = (f as InvalidData)
        assert(i.cause is NoTransformationFoundException)
        val req = server.takeRequest()
        val url = req.requestUrl!!
        assertEquals(roomId, url.pathSegments[5])
        assertEquals("token", url.queryParameter("access_token"))
        assertEquals(from, url.queryParameter("from"))
        assertEquals("b", url.queryParameter("dir"))
        assertEquals("10", url.queryParameter("limit"))

        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(messagesResponse)
        )
        val messages = runBlocking {
            api.getRoomMessages(RoomId(roomId), from, FetchDirection.Backward)
        }
        assert(messages.isSuccess) { "Room $messages"}
        val m = messages.getOrThrow()
        assertEquals("$143273582443PhrSn:example.org", m.chunk[0].value.event_id)
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
        val query = RoomDirectoryQuery(RoomDirectoryFilter(term), limit = 20)
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
        assertEquals(term, q1.filter!!.generic_search_term)
    }

    @Ignore("need local server")
    @Test
    fun testGetPublicRoomsLocal() {
        val base = "http://localhost:8008".toHttpUrlOrNull()!!
        val client = KHttpClient.client
        val s = Server(base, client)
        val userId = System.getenv("MATRIX_USERID")
        val token = System.getenv("MATRIX_TOKEN")
        val api = s.account(UserId(userId), token)
        val n1 = runBlocking {
            api.listPublicRooms(limit=22)
        }
        assert(n1.isSuccess) { "Expected $n1"}
    }
    @Ignore("need local server")
    @Test
    fun postPublicRoomsLocal() = runBlocking {
        val base = "http://localhost:8008".toHttpUrlOrNull()!!
        val client = KHttpClient.client
        val s = Server(base, client)
        val userId = System.getenv("MATRIX_USERID")
        val token = System.getenv("MATRIX_TOKEN")
        val api = s.account(UserId(userId), token)
        val n1 = api.findPublicRooms(limit=1)
        assert(n1.isSuccess) { "Expected $n1"}
    }
    @Ignore("need local server")
    @Test
    fun testPublicRoomFlowLocal() = runBlocking {
        val base = "http://localhost:8008".toHttpUrlOrNull()!!
        val client = KHttpClient.client
        val s = Server(base, client)
        val userId = System.getenv("MATRIX_USERID")
        val token = System.getenv("MATRIX_TOKEN")
        val api = s.account(UserId(userId), token)
        var total = 0
        var accum = 0
        api.publicRoomFlow(limit=1).collect {
            val l = it.getOrThrow()
            if (total != l.total_room_count_estimate) {
                total = l.total_room_count_estimate
                logger.info { "update total_room_count_estimate to $total" }
            }
            accum += l.chunk.size
        }
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
        val result = runBlocking {
            returnNonLocal()
        }
        assert(result.isFailure)

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
    fun deleteRoomAlias() {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        mockWebServer.enqueue(MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{}"""))
        val base = mockWebServer.url("mock")
        val matrixServer = Server(base, fastClient)
        val api = matrixServer.account(UserId("u"), "token1")
        val n= runBlocking {
            api.deleteRoomAlias("test4alias")
        }
        assert(n.isSuccess)
        val req = mockWebServer.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/mock/_matrix/client/r0/directory/room/test4alias", req.path?.substringBefore('?'))

        val url = req.requestUrl!!
        assertEquals("token1", url.queryParameter("access_token"))
    }

    @Test
    fun createRoom() {
        val server = MockWebServer()
        server.start()
        val base = server.url("mock")
        val s = Server(base, KHttpClient.client)
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json"))
        val token = "secretToken101"
        val api = s.account(UserId("u"), token)
        val aliasName = "milano"
        val settings = CreateRoomSettings(aliasName, RoomVisibility.Private)
        runBlocking {
            val n= api.createRoom(settings)
            assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is InvalidData) { "fail $f"}
            assertTrue((f as InvalidData).cause is JsonDecodingException)
        }
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        val url = req.requestUrl!!
        assertEquals("createRoom", url.pathSegments[4])
        assertEquals(token, url.queryParameter("access_token"))

        val body = req.body.readUtf8()
        assertEquals("""{"room_alias_name":"milano","visibility":"private"}""", body)
        
        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{}""")
        )
        val response = runBlocking { api.createRoom(CreateRoomSettings("roomAlias2", RoomVisibility.Public) )}
        assert(response.isFailure)
        val f1 = response.failureOrThrow()
        assert(f1 is InvalidData) { "fail $f1"}
        assert((f1 as InvalidData).cause is MissingFieldException)

        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{}""")
                .setResponseCode(404)
        )
        val response1 = runBlocking { api.createRoom(CreateRoomSettings("roomAlias3", RoomVisibility.Private)) }
        assert(response1.isFailure) { "fail $response1" }
        val f = response1.failureOrThrow()
        f as HttpFailure
        assertEquals(404, f.http_code)

        val createdRoom = "someRoomId0"
        server.enqueue(MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"room_id": "$createdRoom"}""")
        )
        val response2 = runBlocking { api.createRoom(CreateRoomSettings("roomAlias2", RoomVisibility.Public) )}
        assert(response2.isSuccess) { "fail $response2"}
        val created = response2.getOrThrow()
        assertEquals(createdRoom, created.room_id.full)

        assertEquals("POST", server.takeRequest().method)
    }

    @Test
    fun getNotifications() {
        val server = MockWebServer()
        server.start()
        val base = server.url("v100")
        val s = Server(base, fastClient)
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
                .setBody(notificationResponse1)
        )
        val api = s.account(UserId("u1"), "secrettoken1")
        val n = runBlocking { api.getNotifications("from1", limit = 7, only = "filter1") }
        val req = server.takeRequest()
        val url = req.requestUrl!!
        assertEquals("/v100/_matrix/client/r0/notifications", url.encodedPath)
        assertEquals("secrettoken1", url.queryParameter("access_token"))
        assertEquals("from1", url.queryParameter("from"))
        assertEquals("filter1", url.queryParameter("only"))
        assertEquals("7", url.queryParameter("limit"))
        assert(n.isSuccess) { "expected $n"}
        val notification1 = n.getOrThrow()
        assertEquals("abcdef", notification1.next_token)
        assertEquals(1, notification1.notifications.size)
        assertEquals("hcbvkzxhcvb", notification1.notifications[0].profile_tag)
    }

    companion object {
        val messagesResponse = """
        {
          "start": "t47429-4392820_219380_26003_2265",
          "end": "t47409-4357353_219380_26003_2265",
          "chunk": [
            {
              "content": {
                "body": "This is an example text message",
                "msgtype": "m.text",
                "format": "org.matrix.custom.html",
                "formatted_body": "<b>This is an example text message</b>"
              },
              "type": "m.room.message",
              "event_id": "${'$'}143273582443PhrSn:example.org",
              "room_id": "!636q39766251:example.com",
              "sender": "@example:example.org",
              "origin_server_ts": 1432735824653,
              "unsigned": {
                "age": 1234
              }
            },
            {
              "content": {
                "name": "The room name"
              },
              "type": "m.room.name",
              "event_id": "${'$'}143273582443PhrSn:example.org",
              "room_id": "!636q39766251:example.com",
              "sender": "@example:example.org",
              "origin_server_ts": 1432735824653,
              "unsigned": {
                "age": 1234
              },
              "state_key": ""
            },
            {
              "content": {
                "body": "Gangnam Style",
                "url": "mxc://example.org/a526eYUSFFxlgbQYZmo442",
                "info": {
                  "thumbnail_url": "mxc://example.org/FHyPlCeYUSFFxlgbQYZmoEoe",
                  "thumbnail_info": {
                    "mimetype": "image/jpeg",
                    "size": 46144,
                    "w": 300,
                    "h": 300
                  },
                  "w": 480,
                  "h": 320,
                  "duration": 2140786,
                  "size": 1563685,
                  "mimetype": "video/mp4"
                },
                "msgtype": "m.video"
              },
              "type": "m.room.message",
              "event_id": "${'$'}143273582443PhrSn:example.org",
              "room_id": "!636q39766251:example.com",
              "sender": "@example:example.org",
              "origin_server_ts": 1432735824653,
              "unsigned": {
                "age": 1234
              }
            }
          ]
        }
    """.trimIndent()
    }
    val notificationResponse1 = """
        {
  "next_token": "abcdef",
  "notifications": [
    {
      "actions": [
        "notify"
      ],
      "profile_tag": "hcbvkzxhcvb",
      "read": true,
      "room_id": "!abcdefg:example.com",
      "ts": 1475508881945,
      "event": {
        "content": {
          "body": "This is an example text message",
          "msgtype": "m.text",
          "format": "org.matrix.custom.html",
          "formatted_body": "<b>This is an example text message</b>"
        },
        "type": "m.room.message",
        "event_id": "${'$'}143273582443PhrSn:example.org",
        "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
        "sender": "@example:example.org",
        "origin_server_ts": 1432735824653,
        "unsigned": {
          "age": 1234
        }
      }
    }
  ]
}
    """
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
