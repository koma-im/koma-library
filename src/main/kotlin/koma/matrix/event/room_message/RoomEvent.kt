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
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonOutput

// try to make moshi return different kind of objects depending on a key

@Serializable(with = RoomEventSerializer::class)
sealed class RoomEvent: Comparable<RoomEvent>{
    abstract val event_id: EventId
    abstract val origin_server_ts: Long
    abstract val type: RoomEventType

    fun stringifyPretty(): String {
        return jsonPretty.stringify(RoomEvent.serializer(), this)
    }

    companion object {
        fun parseOrNull(json: String): RoomEvent? {
            return try {
                jsonDefault.parse(RoomEvent.serializer(), json)
            } catch (e: Exception) {
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
        val prev_content:JsonObject?,
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
            StringDescriptor.withName("RoomEvent")

    override fun serialize(encoder: Encoder, obj: RoomEvent) {
        val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
        val tree = when (obj) {
            is MRoomMessage-> output.json.toJson(MRoomMessage.serializer(), obj)
            is MRoomMember -> output.json.toJson(MRoomMember.serializer(), obj)
            is MRoomName-> output.json.toJson(MRoomName.serializer(), obj)
            is MRoomAvatar -> output.json.toJson(MRoomAvatar.serializer(), obj)
            is MRoomTopic -> output.json.toJson(MRoomTopic.serializer(), obj)
            is MRoomHistoryVisibility -> output.json.toJson(MRoomHistoryVisibility.serializer(), obj)
            is MRoomGuestAccess -> output.json.toJson(MRoomGuestAccess.serializer(), obj)
            is MRoomCreate -> output.json.toJson(MRoomCreate.serializer(), obj)
            is MRoomAliases -> output.json.toJson(MRoomAliases.serializer(), obj)
            is MRoomCanonAlias -> output.json.toJson(MRoomCanonAlias.serializer(), obj)
            is MRoomJoinRule -> output.json.toJson(MRoomJoinRule.serializer(), obj)
            is MRoomPinnedEvents -> output.json.toJson(MRoomPinnedEvents.serializer(), obj)
            is MRoomPowerLevels -> output.json.toJson(MRoomPowerLevels.serializer(), obj)
            is MRoomRedaction -> output.json.toJson(MRoomRedaction.serializer(), obj)
            is MRoomUnrecognized-> output.json.toJson(MRoomUnrecognized.serializer(), obj)
        }
        output.encodeJson(tree)
    }

    override fun deserialize(decoder: Decoder): RoomEvent {
        val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val type = input.json.fromJson(RoomEventType.serializer(), tree.get("type")!!)
        val event = when (type) {
            RoomEventType.Message -> input.json.fromJson(MRoomMessage.serializer(), tree)
            RoomEventType.Member -> input.json.fromJson(MRoomMember.serializer(), tree)
            RoomEventType.Name -> input.json.fromJson(MRoomName.serializer(), tree)
            RoomEventType.HistoryVisibility -> input.json.fromJson(MRoomHistoryVisibility.serializer(), tree)
            RoomEventType.GuestAccess -> input.json.fromJson(MRoomGuestAccess.serializer(), tree)
            RoomEventType.Create -> input.json.fromJson(MRoomCreate.serializer(), tree)
            RoomEventType.Aliases -> input.json.fromJson(MRoomAliases.serializer(), tree)
            RoomEventType.CanonAlias -> input.json.fromJson(MRoomCanonAlias.serializer(), tree)
            RoomEventType.JoinRule -> input.json.fromJson(MRoomJoinRule.serializer(), tree)
            RoomEventType.PinnedEvents -> input.json.fromJson(MRoomPinnedEvents.serializer(), tree)
            RoomEventType.PowerLevels -> input.json.fromJson(MRoomPowerLevels.serializer(), tree)
            RoomEventType.Redaction -> input.json.fromJson(MRoomRedaction.serializer(), tree)
            RoomEventType.Unknown-> input.json.fromJson(MRoomUnrecognized.serializer(), tree)
            RoomEventType.Topic -> input.json.fromJson(MRoomTopic.serializer(), tree)
            RoomEventType.Avatar -> input.json.fromJson(MRoomAvatar.serializer(), tree)
            RoomEventType.BotOptions -> input.json.fromJson(MRoomUnrecognized.serializer(), tree)
        }
        return event
    }
}