package koma.matrix.user

import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.list
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

private  typealias Data = AvatarUrl
internal class AvatarUrlTest {


    @Test
    fun getAvatar_url() {
        val json = Json(JsonConfiguration.Stable)
        val jsonData = json.stringify(Data.serializer(), Data("a0"))
        assertEquals("""{"avatar_url":"a0"}""", jsonData)
        val jsonList = json.stringify(Data.serializer().list, listOf(Data("a1")))
        assertEquals("""[{"avatar_url":"a1"}]""", jsonList)

        // parsing data back
        val obj = json.parse(Data.serializer(), """{"avatar_url":42}""")
        assertEquals("42", obj.avatar_url)
        assertThrows<JsonDecodingException> { json.parse(Data.serializer(), """{"avatar_url":42, "key": 1}""")  }
    }
}