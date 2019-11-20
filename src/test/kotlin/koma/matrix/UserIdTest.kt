package koma.matrix

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import koma.IOFailure
import koma.matrix.json.MoshiInstance
import koma.matrix.json.deserialize
import koma.util.getOrThrow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.list
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class UserIdTest {

    @Test
    fun testKtSerialization() {
        val json = Json(JsonConfiguration.Stable)
        val jsonUserId = json.stringify(UserId.serializer(), UserId("a0"))
        assertEquals(""""a0"""", jsonUserId)
        val jsonList = json.stringify(UserId.serializer().list, listOf(UserId("a1")))
        assertEquals("""["a1"]""", jsonList)

        // parsing UserId back
        val obj = json.parse(UserId.serializer(), """u2""")
        assertEquals("u2", obj.full)
    }

    @Test
    fun testMoshiSerialization() {
        val adapter = MoshiInstance.moshi.adapter<UserId>(UserId::class.java)
        val jsonUserId = adapter.toJson(UserId("a0"))
        assertEquals(""""a0"""", jsonUserId)

        // must quote the string
        val obj: UserId = deserialize<UserId>(""""u2"""").getOrThrow()
        assertEquals("u2", obj.full)
        val (success, failure) = deserialize<UserId>("""u2""")
        assertNull(success)
        assert(failure is IOFailure && failure.throwable is JsonEncodingException)
    }

}