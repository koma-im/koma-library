package koma.matrix.json

import com.squareup.moshi.Types
import koma.matrix.event.room_message.state.RoomCanonAliasContent
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

        val typeA = Types.newParameterizedType(RawJson::class.java, RoomCanonAliasContent::class.java)
        val adapter3 = MoshiInstance.moshi.adapter<RawJson<RoomCanonAliasContent>>(typeA)
        val data3 = adapter3.fromJson(aliasJson)
        assertNotNull(data3)
        assertEquals("v", data3!!.raw["k"])
    }
}
