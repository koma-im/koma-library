package koma.matrix

import com.squareup.moshi.*
import koma.matrix.event.EventId
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.json.MoshiInstance
import koma.matrix.room.naming.RoomId

data class NotificationResponse(
        val next_token: String?,
        val notifications: List<Notification>
) {

    data class Notification(
            val actions: List<Any>,
            val event: Event<Content>,
            val profile_tag: String,
            val read: Boolean,
            val room_id: RoomId,
            val ts: Long
    ) {
    }

    data class Event<T>(
            val event_id: EventId,
            val origin_server_ts: Long,
            val prev_content: Map<String, Any>?,
            val sender: UserId,
            val state_key: String?,
            /**
             * null when the type isn't supported yet
             */
            val type: RoomEventType?,
            val content: T,
            val unsigned: Unsigned?
    ) {
    }

    data class Unsigned(
            val age: Long,
            val prev_content: Event<Content>,
            val transaction_id: String?,
            val redacted_because: Event<Content>
    )

    sealed class Content {
        data class Message(val message: M_Message): Content()
        data class Other(val map: Map<String, Any>): Content()
        companion object {
            private val messageAdapter = MoshiInstance.moshi.adapter<M_Message>(M_Message::class.java)
            internal fun fromMap(type: RoomEventType?, map: Map<String, Any>): Content? {
                return when (type) {
                    RoomEventType.Message -> {
                        val m = messageAdapter.fromJsonValue(map) ?: return null
                        Message(m)
                    }
                    else -> Other(map)
                }
            }
            internal fun toMap(content: Content): Map<String, Any> = when(content) {
                is Message -> messageAdapter.toJsonValue(content.message) as Map<String, Any>
                is Other -> content.map
            }
        }
    }

    internal class EventAdapter {
        @ToJson
        fun toJson(value: Event<Content>): Event<Map<String, Any>> {
            val c = Content.toMap(value.content)
            return Event(event_id = value.event_id,
                    origin_server_ts = value.origin_server_ts,
                    prev_content = value.prev_content,
                    sender = value.sender,
                    type = value.type,
                    content = c,
                    state_key = value.state_key,
                    unsigned = value.unsigned
            )
        }
        @FromJson
        fun fromJson(json: Event<Map<String, Any>>): Event<Content>? {
            val c = Content.fromMap(json.type, json.content) ?: return null
            return Event(event_id = json.event_id,
                    origin_server_ts = json.origin_server_ts,
                    prev_content = json.prev_content,
                    sender = json.sender,
                    type = json.type,
                    content = c,
                    state_key = json.state_key,
                    unsigned = json.unsigned)
        }
    }
}