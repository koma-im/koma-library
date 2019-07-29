package koma.network.media

import okhttp3.HttpUrl
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class DownloadKtTest {

    @Test
    fun parseMxc() {
        assertEquals(MHUrl.Http(HttpUrl.parse("http://matrix,org")!!),
                "http://matrix,org".parseMxc())
        assertEquals(null, "htt://matrix,org".parseMxc())
        assertEquals(
                MHUrl.Mxc("s0", "bylikennIyhLnpOcHTxtFKVa"),
                "mxc://s0/bylikennIyhLnpOcHTxtFKVa".parseMxc())
        assertEquals(MHUrl.Mxc("a", "b"), "mxc://a/b".parseMxc())
        assertEquals(MHUrl.Mxc("a", "b"), "mxc://a/b/".parseMxc())
        assertEquals(MHUrl.Mxc("a", "b"), "mxc://a/b/?".parseMxc())
    }
}