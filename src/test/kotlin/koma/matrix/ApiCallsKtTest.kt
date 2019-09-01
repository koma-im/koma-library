package koma.matrix

import koma.Koma
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.room.naming.RoomId
import koma.network.client.okhttp.AppHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.net.Proxy

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
        val s = km.server(HttpUrl.parse("http://server")!!)
        val a = s.account(UserId("uid"), "token")
        runBlocking {
            a.sendMessage(RoomId("room"), TextMessage("msg"))
        }
    }
}