package koma.matrix

import koma.Koma
import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.room.naming.RoomId
import koma.network.client.okhttp.AppHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.lang.IllegalArgumentException
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
}