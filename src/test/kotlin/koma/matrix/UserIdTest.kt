package koma.matrix

import koma.IOFailure
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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

        val u = json.parse(UserId.serializer(), """"u2"""")
        assertEquals("u2", u.full)
    }
}