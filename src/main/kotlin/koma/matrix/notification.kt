package koma.matrix

import koma.matrix.event.EventId
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonOutput

@Serializable
data class NotificationResponse(
        val next_token: String? = null,
        val notifications: List<Notification>
) {

    @Serializable
    data class Notification(
            val actions: List<JsonElement>,
            val event: Event,
            val profile_tag: String? = null,
            val read: Boolean,
            val room_id: RoomId,
            val ts: Long
    ) {
    }

    @Serializable
    class Event(
            val event_id: EventId,
            val origin_server_ts: Long,
            val prev_content: JsonObject?= null,
            val sender: UserId,
            val state_key: String? = null,
            val type: RoomEventType,
            val content: Content,
            val unsigned: Unsigned? = null
    ) {
        @Serializer(forClass = Event::class)
        companion object : KSerializer<Event> {
            override val descriptor: SerialDescriptor =
                    StringDescriptor.withName("Event")

            override fun serialize(encoder: Encoder, obj: Event) {
                val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
                val contentObject = obj.content.toJson(output)
                val pri = PrimevalEvent.copyFrom(obj, contentObject)
                output.encode(PrimevalEvent.serializer(), pri)
            }

            override fun deserialize(decoder: Decoder): Event {
                val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
                val pri = input.decode(PrimevalEvent.serializer())
                val content = Content.fromJson(input, pri.type, pri.content)
                return copyFrom(pri, content)
            }

            private fun copyFrom(other: PrimevalEvent, content: Content) = Event(
                    event_id = other.event_id,
                    origin_server_ts = other.origin_server_ts,
                    prev_content = other.prev_content,
                    sender = other.sender,
                    state_key = other.state_key,
                    type = other.type,
                    content = content,
                    unsigned = other.unsigned
            )
        }
    }

    @Serializable
    internal class PrimevalEvent(
            val event_id: EventId,
            val origin_server_ts: Long,
            val prev_content: JsonObject?= null,
            val sender: UserId,
            val state_key: String?=null,
            val type: RoomEventType,
            val content: JsonObject,
            val unsigned: Unsigned? = null
    ) {
        companion object {
            internal fun copyFrom(other: Event, content: JsonObject) = PrimevalEvent(
                    event_id = other.event_id,
                    origin_server_ts = other.origin_server_ts,
                    prev_content = other.prev_content,
                    sender = other.sender,
                    state_key = other.state_key,
                    type = other.type,
                    content = content,
                    unsigned = other.unsigned
            )
        }
    }

    @Serializable
    data class Unsigned(
            val age: Long,
            val prev_content: Event? = null,
            val transaction_id: String? = null,
            val redacted_because: Event? = null
    )

    sealed class Content {
        data class Message(val message: M_Message): Content()
        data class Other(val map: JsonObject): Content()
        internal fun toJson(output: JsonOutput): JsonObject {
            return when (this) {
                is Message -> output.json.toJson(M_Message.serializer(), this.message) as JsonObject
                is Other -> this.map
            }
        }
        companion object {
            internal fun fromJson(input: JsonInput, type: RoomEventType?, map: JsonObject): Content {
                return when (type) {
                    RoomEventType.Message -> {
                        val m = input.json.fromJson(M_Message.serializer(), map)
                        Message(m)
                    }
                    else -> Other(map)
                }
            }
        }
    }
}