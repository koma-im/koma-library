package koma.matrix.user

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.SerializationException as JsonDecodingException
import kotlinx.serialization.list
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

private  typealias Data = AvatarUrl
internal class AvatarUrlTest {


    @Test
    fun getAvatar_url() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonData = json.encodeToString(Data.serializer(), Data("a0"))
        assertEquals("""{"avatar_url":"a0"}""", jsonData)
        val jsonList = json.encodeToString(ListSerializer(Data.serializer()), listOf(Data("a1")))
        assertEquals("""[{"avatar_url":"a1"}]""", jsonList)

        // parsing data back
        val obj = json.decodeFromString(Data.serializer(), """{"avatar_url":42}""")
        assertEquals("42", obj.avatar_url)
        assertThrows<JsonDecodingException> { json.decodeFromString(Data.serializer(), """{"avatar_url":42, "key": 1}""") }
    }
}