package koma.matrix.event.room_message

import koma.matrix.json.jsonDefault
import kotlinx.serialization.internal.nullable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RoomEventTypeTest {

    @Test
    fun testToString() {
        val json = Json(JsonConfiguration.Stable)
        val jsonUserId = json.stringify(RoomEventType.serializer(), RoomEventType.Aliases)
        assertEquals(""""m.room.aliases"""", jsonUserId)
    }
    @Test
    fun testEnumStr() {
        val a = jsonDefault.parse(RoomEventType.serializer(), "m.room.aliases")
        assertEquals(RoomEventType.Aliases, a)
        val u = jsonDefault.parse(RoomEventType.serializer(), "mx")
        assertEquals(RoomEventType.Unknown, u)
        val s = RoomEventType.enumToStr(RoomEventType.Aliases)
        assertEquals("m.room.aliases", s)
    }
}