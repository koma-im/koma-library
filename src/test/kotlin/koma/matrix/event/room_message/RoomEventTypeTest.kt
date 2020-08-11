package koma.matrix.event.room_message

import koma.matrix.json.jsonDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RoomEventTypeTest {

    @Test
    fun testToString() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonUserId = json.encodeToString(RoomEventType.serializer(), RoomEventType.Aliases)
        assertEquals(""""m.room.aliases"""", jsonUserId)
    }
    @Test
    fun testEnumStr() {
        val a = jsonDefault.decodeFromString(RoomEventType.serializer(), "m.room.aliases")
        assertEquals(RoomEventType.Aliases, a)
        val u = jsonDefault.decodeFromString(RoomEventType.serializer(), "mx")
        assertEquals(RoomEventType.Unknown, u)
        val s = RoomEventType.Aliases.toName()
        assertEquals("m.room.aliases", s)
    }
}