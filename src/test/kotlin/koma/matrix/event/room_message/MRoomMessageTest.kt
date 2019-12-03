package koma.matrix.event.room_message

import koma.matrix.event.room_message.chat.TextMessage
import koma.matrix.json.jsonDefault
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MRoomMessageTest {

    @Test
    fun getRoomMessage() {
        val msgText = """{
      "content": {
        "body": "This is an example text message",
        "msgtype": "m.text",
        "format": "org.matrix.custom.html",
        "formatted_body": "<b>This is an example text message</b>"
      },
      "type": "m.room.message",
      "event_id": "${'$'}143273582443PhrSn:example.org",
      "room_id": "!636q39766251:example.com",
      "sender": "@example:example.org",
      "origin_server_ts": 1432735824653,
      "unsigned": {
        "age": 1234
      }
    }"""
        val msg = jsonDefault.parse(MRoomMessage.serializer(), msgText)
        assertEquals(RoomEventType.Message, msg.type)
        assertEquals(1432735824653, msg.origin_server_ts)
        assertEquals("!636q39766251:example.com", msg.room_id?.full)
        assertEquals("@example:example.org", msg.sender.full)
        assertEquals("<b>This is an example text message</b>", (msg.content as TextMessage).formatted_body)
    }
}