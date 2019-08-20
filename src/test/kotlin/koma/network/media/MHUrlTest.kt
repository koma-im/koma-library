package koma.network.media

import okhttp3.HttpUrl
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class MHUrlTest {
    @Test
    fun toString1() {
        val u1 = MHUrl.Mxc("aHK", "b")
        assertEquals(u1, MHUrl.Mxc("aHK", "b"))
        assertEquals(u1.hashCode(), MHUrl.Mxc("aHK", "b").hashCode())
        val u2 = MHUrl.Mxc("c", "bd")
        assertNotEquals(u1, u2)
        val u3 = MHUrl.Http(HttpUrl.parse("http://matrix.org")!!)
        assertEquals(u3,MHUrl.Http(HttpUrl.parse("http://matrix.org")!!))
        assertEquals(u3.hashCode(),MHUrl.Http(HttpUrl.parse("http://matrix.org")!!).hashCode())
        assertNotEquals(u2, u3)
    }

    @Test
    fun testToString() {
        val u1 = MHUrl.Mxc("aHK", "b")
        assertEquals(u1.toString(), "mxc://aHK/b")
        val u2 = MHUrl.Mxc("c", "bd")
        assertEquals("mxc://c/bd", u2.toString())
        val u3 = MHUrl.Http(HttpUrl.parse("http://matrix.org")!!)
        assertEquals("http://matrix.org/", u3.toString())
    }
}