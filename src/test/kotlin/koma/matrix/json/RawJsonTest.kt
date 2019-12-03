package koma.matrix.json

import com.squareup.moshi.Types
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.room.naming.RoomAlias
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.content
import kotlinx.serialization.parse
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class RawJsonTest {
    @Test
    fun test1() {
        val aliasJson = """
            {
                "alias": "#welc2:club",
                "k": "v"              }
        """.trimIndent()
        val data3 = jsonDefault.parse(RawSerializer(RoomCanonAliasContent.serializer()), aliasJson)
        assertNotNull(data3)
        assertEquals("v", data3!!.raw["k"]?.content)
        val alias = data3.value
        assertEquals("#welc2:club", alias.alias?.full)

        val string2 = """
            {"content": {
                "alias": "#alias2:example",
                "k": "v"
                }
            }
        """.trimMargin()
        val data = jsonDefault.parse(AliasContentWrapper.serializer(), string2)
        assertEquals("#alias2:example", data.content.value.alias?.full)
    }

    @Serializable
    private data class AliasContentWrapper(
            @Serializable(with = RawSerializer::class)
            val content: Preserved<RoomCanonAliasContent>
    )
}
