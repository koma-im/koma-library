package koma.matrix.room


import com.squareup.moshi.Types
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.json.MoshiInstance
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
        val data1 = jsonDefault.parse(RoomCanonAliasContent.serializer(), aliasJson)
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
        val adapter2 = MoshiInstance.moshi.adapter(InviteEvent::class.java)
        val data2 = adapter2.fromJson(state)
        assert(data2 is InviteEvent)

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

        val typeA = Types.newParameterizedType(Events::class.java, InviteEvent::class.java)
        val adapter3 = MoshiInstance.moshi.adapter<Events<InviteEvent>>(typeA)
        val data3 = adapter3.fromJson(events)
        assert(data3 is Events<InviteEvent>)


        val adapter = MoshiInstance.moshi.adapter<InvitedRoom>(InvitedRoom::class.java)
        val data = adapter.fromJson(invitedRoom)
        assert(data is InvitedRoom)
    }
}
