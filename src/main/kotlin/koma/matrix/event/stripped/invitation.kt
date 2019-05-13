package koma.matrix.event.stripped

import com.squareup.moshi.*
import koma.matrix.UserId
import koma.matrix.event.room_message.JsonKeyFinder
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.event.room_message.state.*
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.room.InviteEvent
import java.lang.reflect.Type

internal class InviteEventAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<InviteEvent>? {
        if (annotations.isNotEmpty() || Types.getRawType(type) != InviteEvent::class.java) {
            return null
        }
        return InviteEventAdapter(moshi)
    }
}

private class InviteEventAdapter(m: Moshi): JsonAdapter<InviteEvent>() {
    private val mapAdapter = m.adapter(Map::class.java)

    private val createSubAdapter: (Class<out Any>) -> JsonAdapter<Any> = {
        m.adapter<Any>(it)
    }
    private val keyToAdapters = mapOf(
            RoomEventType.CanonAlias to createSubAdapter(RoomCanonAliasContent::class.java),
            RoomEventType.JoinRule to createSubAdapter(RoomJoinRulesContent::class.java),
            RoomEventType.Member to createSubAdapter(RoomMemberContent::class.java),
            RoomEventType.Name to createSubAdapter(RoomNameContent::class.java),
            RoomEventType.Topic to createSubAdapter(RoomTopicContent::class.java),
            RoomEventType.Avatar to createSubAdapter(RoomAvatarContent::class.java)
    )
    private val keyFinder = JsonKeyFinder("type")

    override fun toJson(writer: JsonWriter, msg: InviteEvent?) {
        msg ?: return
        mapAdapter.toJson(writer, mapOf<String, Any>(
                "sender" to msg.sender,
                "type" to msg.type,
                "state_key" to (msg.state_key ?: ""),
                "content" to msg.content
        ))
    }

    override fun fromJson(jr: JsonReader): InviteEvent? {
        val k = keyFinder.find(jr.peekJson())
        val type = k?.let { RoomEventType.strToEnum(it)} ?: return null
        val adapter = keyToAdapters.get(type) ?: mapAdapter
        var sender: String? = null
        var stateKey: String? = null
        var content: Any? = null
        jr.beginObject()
        loop@ while (jr.hasNext()) {
            when (jr.nextName()) {
                "sender" -> sender = jr.nextString()
                "state_key" -> stateKey = jr.nextString()
                "content" -> content = adapter.fromJson(jr)
                else -> {
                    jr.skipValue()
                    continue@loop
                }
            }
        }
        jr.endObject()
        return InviteEvent (
                sender = sender?.let { UserId(it) } ?: return null,
                type = type,
                state_key = stateKey ?: return null,
                content = content ?: return null
        )
    }
}
