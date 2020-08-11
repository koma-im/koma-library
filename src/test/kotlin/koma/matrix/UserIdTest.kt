package koma.matrix

import koma.IOFailure
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class UserIdTest {

    @Test
    fun testKtSerialization() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonUserId = json.encodeToString(UserId.serializer(), UserId("a0"))
        assertEquals(""""a0"""", jsonUserId)
        val jsonList = json.encodeToString(ListSerializer(UserId.serializer()), listOf(UserId("a1")))
        assertEquals("""["a1"]""", jsonList)

        // parsing UserId back
        val obj = json.decodeFromString(UserId.serializer(), """u2""")
        assertEquals("u2", obj.full)

        val u = json.decodeFromString(UserId.serializer(), """"u2"""")
        assertEquals("u2", u.full)
    }
}