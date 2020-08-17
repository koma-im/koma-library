package koma.matrix.room

import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.json.jsonDefault
import koma.matrix.sync.Events
import kotlin.test.Test

val invitedRoom = """{
          "invite_state": {
              "events": [
              {
              "content": {
                "membership": "join",
                "avatar_url": null,
                "displayname": "newu1"
              },
              "type": "m.room.member",
              "sender": "@newu1:club",
              "state_key": "@newu1:club"
            },
            {
              "content": {
                "join_rule": "public"
              },
              "type": "m.room.join_rules",
              "sender": "@newu1:club",
              "state_key": ""
            },
            {
              "content": {
                "alias": "#welc2:club"
              },
              "type": "m.room.canonical_alias",
              "sender": "@newu1:club",
              "state_key": ""
            },
            {
              "content": {
                "mpk": "mapvalue"
              },
              "type": "m.room.bot.options",
              "sender": "@newu1:club",
              "state_key": ""
            }
            ]
          }
        }
        """.trimIndent()

internal class InvitationDeserialization {
    @Test
    fun test1() {
        val aliasJson = """
            {
                "alias": "#welc2:club"
              }
        """.trimIndent()
        val data1 = jsonDefault.decodeFromString(RoomCanonAliasContent.serializer(), aliasJson)
        assert(data1 is RoomCanonAliasContent)

        val state = """
            {
              "content": {
                "alias": "#welc2:club"
              },
              "type": "m.room.canonical_alias",
              "sender": "@newu1:club",
              "state_key": ""
            }
        """.trimIndent()
        val data2 =jsonDefault.decodeFromString(InviteEvent.serializer(), state)

        val events = """
          {
            "events": [
            {
              "content": {
                "alias": "#welc2:club"
              },
              "type": "m.room.canonical_alias",
              "sender": "@newu1:club",
              "state_key": ""
            }
            ]
          }
        """.trimIndent()
        val data3 =jsonDefault.decodeFromString(Events.serializer(InviteEvent.serializer()), events)

        jsonDefault.decodeFromString(InvitedRoom.serializer(), invitedRoom)
    }
}
