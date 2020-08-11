package koma.matrix.event.context

import koma.matrix.json.jsonDefault
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ContextResponseTest {

    @Test
    fun getEnd() {
        val context = jsonDefault.decodeFromString(ContextResponse.serializer(), contextExample1)
    }

    private val contextExample1 = """{
  "end": "t29-57_2_0_2",
  "events_after": [
    {
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
    }
  ],
  "event": {
    "content": {
      "body": "filename.jpg",
      "info": {
        "h": 398,
        "w": 394,
        "mimetype": "image/jpeg",
        "size": 31037
      },
      "url": "mxc://example.org/JWEIFJgwEIhweiWJE",
      "msgtype": "m.image"
    },
    "type": "m.room.message",
    "event_id": "${'$'}f3h4d129462ha:example.com",
    "room_id": "!636q39766251:example.com",
    "sender": "@example:example.org",
    "origin_server_ts": 1432735824653,
    "unsigned": {
      "age": 1234
    }
  },
  "events_before": [
    {
      "content": {
        "body": "something-important.doc",
        "filename": "something-important.doc",
        "info": {
          "mimetype": "application/msword",
          "size": 46144
        },
        "msgtype": "m.file",
        "url": "mxc://example.org/FHyPlCeYUSFFxlgbQYZmoEoe"
      },
      "type": "m.room.message",
      "event_id": "${'$'}143273582443PhrSn:example.org",
      "room_id": "!636q39766251:example.com",
      "sender": "@example:example.org",
      "origin_server_ts": 1432735824653,
      "unsigned": {
        "age": 1234
      }
    }
  ],
  "start": "t27-54_2_0_2",
  "state": [
    {
      "content": {
        "creator": "@example:example.org",
        "room_version": "1",
        "m.federate": true,
        "predecessor": {
          "event_id": "${'$'}something:example.org",
          "room_id": "!oldroom:example.org"
        }
      },
      "type": "m.room.create",
      "event_id": "${'$'}143273582443PhrSn:example.org",
      "room_id": "!636q39766251:example.com",
      "sender": "@example:example.org",
      "origin_server_ts": 1432735824653,
      "unsigned": {
        "age": 1234
      },
      "state_key": ""
    },
    {
      "content": {
        "membership": "join",
        "avatar_url": "mxc://example.org/SEsfnsuifSDFSSEF",
        "displayname": "Alice Margatroid"
      },
      "type": "m.room.member",
      "event_id": "${'$'}143273582443PhrSn:example.org",
      "room_id": "!636q39766251:example.com",
      "sender": "@example:example.org",
      "origin_server_ts": 1432735824653,
      "unsigned": {
        "age": 1234
      },
      "state_key": "@alice:example.org"
    }
  ]
}"""
}