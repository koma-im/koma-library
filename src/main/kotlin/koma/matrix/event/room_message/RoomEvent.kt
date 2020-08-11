package koma.matrix.event.room_message

import koma.matrix.UserId
import koma.matrix.event.EventId
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.chat.MessageUnsigned
import koma.matrix.event.room_message.state.*
import koma.matrix.event.room_message.state.member.PrevContent
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.event.room_message.state.member.RoomMemberUnsigned
import koma.matrix.event.room_message.state.member.StrippedState
import koma.matrix.json.jsonDefault
import koma.matrix.json.jsonPretty
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
// try to make moshi return different kind of objects depending on a key

@Serializable(with = RoomEventSerializer::class)
sealed class RoomEvent: Comparable<RoomEvent>{
    abstract val event_id: EventId
    abstract val origin_server_ts: Long
    abstract val type: RoomEventType

    fun stringifyPretty(): String {
        return jsonPretty.encodeToString(RoomEvent.serializer(), this)
    }

    companion object {
        fun parseOrNull(json: String): RoomEvent? {
            return try {
                jsonDefault.decodeFromString(RoomEvent.serializer(), json)
            } catch (e: Exception) {
                logger.error { "Could not parse $json for $e"}
                null
            }
        }
    }
    override fun compareTo(other: RoomEvent): Int {
        return this.origin_server_ts.compareTo(other.origin_server_ts)
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        val om = other as RoomEvent
        return this.event_id == om.event_id
    }

    override fun hashCode() = this.event_id.hashCode()
}

@Serializable
class MRoomMessage(
        //val age: Long?,
        override val  event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val unsigned: MessageUnsigned? = null,
        val room_id: RoomId?=null,
        /**
         * sometimes content can be as empty as {}
         */
        val content: M_Message?=null,
        override val type: RoomEventType = RoomEventType.Message
): RoomEvent() {
    override fun toString(): String {
        return "MRoomMessage(event_id=$event_id, origin_server_ts=$origin_server_ts, prev_content=$prev_content, sender=$sender, unsigned=$unsigned, room_id=$room_id, content=$content, type=$type)"
    }
}

@Serializable
class MRoomRedaction(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val redacts: String,
        val content: RoomRedactContent,
        override val type: RoomEventType = RoomEventType.Redaction
): RoomEvent()

@Serializable
class MRoomUnrecognized(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId? = null,
        val state_key: String? = null,
        val unsigned: MessageUnsigned? = null,
        val content: JsonObject? = null,
        override val type: RoomEventType = RoomEventType.Unknown
): RoomEvent() {
    override fun toString(): String {
        return "MRoomUnrecognized(prev_content=$prev_content, sender=$sender, state_key=$state_key, unsigned=$unsigned, content=$content)"
    }
}


sealed class RoomStateEvent: RoomEvent() {
    abstract override val event_id: EventId
    abstract override val origin_server_ts: Long
    abstract override val type: RoomEventType
}

@Serializable
class MRoomAliases(
        //val age: Long?,
        override val origin_server_ts: Long,
        override val event_id: EventId,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String,
        val content: RoomAliasesContent,
        override val type: RoomEventType = RoomEventType.Aliases
): RoomStateEvent()

@Serializable
class MRoomCanonAlias(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: RoomCanonAliasContent,
        override val type: RoomEventType = RoomEventType.CanonAlias
): RoomStateEvent()

@Serializable
class MRoomCreate(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: RoomCreateContent,
        override val type: RoomEventType = RoomEventType.Create
): RoomStateEvent()

@Serializable
class MRoomJoinRule(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: RoomJoinRulesContent,
        override val type: RoomEventType = RoomEventType.JoinRule
): RoomStateEvent()

@Serializable
class MRoomMember(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: PrevContent? = null,
        val sender: UserId,
        val unsigned: RoomMemberUnsigned? = null,
        val replaces_state: String? = null,
        val state_key: String? = null,
        val invite_room_state: List<StrippedState>? = null,
        val content: RoomMemberContent,
        override val type: RoomEventType = RoomEventType.Member
): RoomStateEvent()

@Serializable
class MRoomPowerLevels(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val content: RoomPowerLevelsContent,
        override val type: RoomEventType = RoomEventType.PowerLevels
): RoomStateEvent()

@Serializable
class MRoomPinnedEvents(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: RoomPinnedEventsContent,
        override val type: RoomEventType = RoomEventType.PinnedEvents
): RoomStateEvent()

@Serializable
class MRoomTopic(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: RoomTopicContent,
        override val type: RoomEventType = RoomEventType.Topic
): RoomStateEvent()

@Serializable
class MRoomName(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content:JsonObject? = null,
        val sender: UserId,
        val state_key: String?= null,
        val txn_id: String?= null,
        val content: RoomNameContent,
        override val type: RoomEventType = RoomEventType.Name
): RoomStateEvent()

@Serializable
class MRoomAvatar(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject?= null,
        val sender: UserId,
        val state_key: String?= null,
        val txn_id: String?= null,
        val content: RoomAvatarContent,
        override val type: RoomEventType = RoomEventType.Avatar
): RoomStateEvent()

@Serializable
class MRoomHistoryVisibility(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject?= null,
        val sender: UserId,
        val state_key: String?= null,
        val txn_id: String?= null,
        val content: RoomHistoryVisibilityContent,
        override val type: RoomEventType = RoomEventType.HistoryVisibility
): RoomStateEvent()

@Serializable
class MRoomGuestAccess(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject?=null,
        val sender: UserId,
        val state_key: String?= null,
        val txn_id: String?= null,
        val content: Content,
        override val type: RoomEventType = RoomEventType.GuestAccess
): RoomStateEvent() {
    @Serializable
    data class Content(
            val guest_access: GuestAccess
    ) {
        enum class GuestAccess{
            can_join,
            forbidden
        }
    }
}

@Serializer(forClass = RoomEvent::class)
internal class RoomEventSerializer: KSerializer<RoomEvent> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RoomEvent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RoomEvent) {
        val obj = value
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json, not $encoder")
        val tree = when (obj) {
            is MRoomMessage-> output.json.encodeToJsonElement(MRoomMessage.serializer(), obj)
            is MRoomMember -> output.json.encodeToJsonElement(MRoomMember.serializer(), obj)
            is MRoomName-> output.json.encodeToJsonElement(MRoomName.serializer(), obj)
            is MRoomAvatar -> output.json.encodeToJsonElement(MRoomAvatar.serializer(), obj)
            is MRoomTopic -> output.json.encodeToJsonElement(MRoomTopic.serializer(), obj)
            is MRoomHistoryVisibility -> output.json.encodeToJsonElement(MRoomHistoryVisibility.serializer(), obj)
            is MRoomGuestAccess -> output.json.encodeToJsonElement(MRoomGuestAccess.serializer(), obj)
            is MRoomCreate -> output.json.encodeToJsonElement(MRoomCreate.serializer(), obj)
            is MRoomAliases -> output.json.encodeToJsonElement(MRoomAliases.serializer(), obj)
            is MRoomCanonAlias -> output.json.encodeToJsonElement(MRoomCanonAlias.serializer(), obj)
            is MRoomJoinRule -> output.json.encodeToJsonElement(MRoomJoinRule.serializer(), obj)
            is MRoomPinnedEvents -> output.json.encodeToJsonElement(MRoomPinnedEvents.serializer(), obj)
            is MRoomPowerLevels -> output.json.encodeToJsonElement(MRoomPowerLevels.serializer(), obj)
            is MRoomRedaction -> output.json.encodeToJsonElement(MRoomRedaction.serializer(), obj)
            is MRoomUnrecognized-> output.json.encodeToJsonElement(MRoomUnrecognized.serializer(), obj)
        }
        output.encodeJsonElement(tree)
    }

    override fun deserialize(decoder: Decoder): RoomEvent {
        val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
        var tree = input.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val tsElement = tree.getValue("origin_server_ts") as JsonPrimitive
        if (tsElement.longOrNull == null) {
            val m = tree.toMutableMap()
            val long = JsonPrimitive(tsElement.double.toLong())
            m["origin_server_ts"] = long
            tree = JsonObject(m)
        }
        val type = input.json.decodeFromJsonElement(RoomEventType.serializer(), tree.get("type")!!)
        val value = tree
        val event = when (type) {
            RoomEventType.Message -> input.json.decodeFromJsonElement(MRoomMessage.serializer(), value)
            RoomEventType.Member -> input.json.decodeFromJsonElement(MRoomMember.serializer(), value)
            RoomEventType.Name -> input.json.decodeFromJsonElement(MRoomName.serializer(), value)
            RoomEventType.HistoryVisibility -> input.json.decodeFromJsonElement(MRoomHistoryVisibility.serializer(), value)
            RoomEventType.GuestAccess -> input.json.decodeFromJsonElement(MRoomGuestAccess.serializer(), value)
            RoomEventType.Create -> input.json.decodeFromJsonElement(MRoomCreate.serializer(), value)
            RoomEventType.Aliases -> input.json.decodeFromJsonElement(MRoomAliases.serializer(), value)
            RoomEventType.CanonAlias -> input.json.decodeFromJsonElement(MRoomCanonAlias.serializer(), value)
            RoomEventType.JoinRule -> input.json.decodeFromJsonElement(MRoomJoinRule.serializer(), value)
            RoomEventType.PinnedEvents -> input.json.decodeFromJsonElement(MRoomPinnedEvents.serializer(), value)
            RoomEventType.PowerLevels -> input.json.decodeFromJsonElement(MRoomPowerLevels.serializer(), value)
            RoomEventType.Redaction -> input.json.decodeFromJsonElement(MRoomRedaction.serializer(), value)
            RoomEventType.Unknown-> input.json.decodeFromJsonElement(MRoomUnrecognized.serializer(), value)
            RoomEventType.Topic -> input.json.decodeFromJsonElement(MRoomTopic.serializer(), value)
            RoomEventType.Avatar -> input.json.decodeFromJsonElement(MRoomAvatar.serializer(), value)
            RoomEventType.BotOptions -> input.json.decodeFromJsonElement(MRoomUnrecognized.serializer(), value)
        }
        return event
    }
}