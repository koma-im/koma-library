package koma.matrix.room

import koma.matrix.UserId
import koma.matrix.event.ephemeral.EphemeralRawEvent
import koma.matrix.event.GeneralEvent
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.event.room_message.state.*
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.json.Preserved
import koma.matrix.json.RawJson
import koma.matrix.json.RawSerializer
import koma.matrix.sync.Events
import koma.matrix.sync.RawMessage
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonOutput

@Serializable
data class JoinedRoom(
        val unread_notifications: UnreadNotifications? = null,
        val summary: RoomSummary? = null,
        val account_data: Events<GeneralEvent>,
        val ephemeral: Events<EphemeralRawEvent>,
        val state: Events<RoomEvent>,
        val timeline: Timeline<@Serializable(with = RawSerializer::class) Preserved<RoomEvent>>
) {
    @Serializable
    class RoomSummary(
            /**
             * Required if the room's m.room.name or m.room.canonical_alias state events are unset or empty.
              */
            @SerialName("m.heroes")
            val heros: List<UserId>? = null,
            /**
             * The number of users with membership of join
             * If this field has not changed since the last sync, it may be omitted. Required otherwise.
             */
            @SerialName("m.joined_member_count")
            val joined_count: Int? = null,
            /**
             * The number of users with membership of invite.
             * If this field has not changed since the last sync, it may be omitted. Required otherwise.
             */
            @SerialName("m.invited_member_count")
            val invited_count: Int? = null
    ) {
        override fun toString(): String {
            return "RoomSummary(heros=$heros, joined_count=$joined_count, invited_count=$invited_count)"
        }
    }
}

@Serializable
data class  Timeline<T>(
        val events: List<T>,
        val limited: Boolean? = null,
        val prev_batch: String? = null
)

@Serializable
data class InvitedRoom(
        val invite_state: Events<InviteEvent>
)

@Serializable
data class InviteEvent(
        // only these 4 keys are allowed
        // this event doesn't replace previous states
        val sender: UserId,
        val type: RoomEventType,
        val state_key: String,
        val content: Any
) {
    @Serializable
    internal class PrimevalInviteEvent(
        // only these 4 keys are allowed
        // this event doesn't replace previous states
        val sender: UserId,
        val type: RoomEventType,
        val state_key: String,
        val content: JsonObject
    )
    @Serializer(forClass = InviteEvent::class)
    companion object : KSerializer<InviteEvent> {
        override val descriptor: SerialDescriptor =
                StringDescriptor.withName("InviteEvent")

        override fun serialize(encoder: Encoder, obj: InviteEvent) {
            val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
            val content = when (obj.content) {
                is RoomMemberContent -> output.json.toJson(RoomMemberContent.serializer(), obj.content)
                is RoomNameContent -> output.json.toJson(RoomNameContent.serializer(), obj.content)
                is RoomCanonAliasContent -> output.json.toJson(RoomCanonAliasContent.serializer(), obj.content)
                is RoomJoinRulesContent -> output.json.toJson(RoomJoinRulesContent.serializer(), obj.content)
                is RoomTopicContent -> output.json.toJson(RoomTopicContent.serializer(), obj.content)
                is RoomAvatarContent -> output.json.toJson(RoomAvatarContent.serializer(), obj.content)
                is JsonObject -> obj.content
                else -> JsonObject(mapOf())
            }
            val pri = PrimevalInviteEvent(obj.sender,
                    type = obj.type,
                    state_key = obj.state_key,
                    content = content as JsonObject)
            encoder.encode(PrimevalInviteEvent.serializer(), pri)
        }

        override fun deserialize(decoder: Decoder): InviteEvent {
            val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
            val pri = decoder.decode(PrimevalInviteEvent.serializer())
            val content: Any = when (pri.type) {
                RoomEventType.Member -> input.json.fromJson(RoomMemberContent.serializer(), pri.content)
                RoomEventType.Name -> input.json.fromJson(RoomNameContent.serializer(), pri.content)
                RoomEventType.CanonAlias -> input.json.fromJson(RoomCanonAliasContent.serializer(), pri.content)
                RoomEventType.JoinRule -> input.json.fromJson(RoomJoinRulesContent.serializer(), pri.content)
                RoomEventType.Topic -> input.json.fromJson(RoomTopicContent.serializer(), pri.content)
                RoomEventType.Avatar -> input.json.fromJson(RoomAvatarContent.serializer(), pri.content)
                else -> pri.content
            }
            return InviteEvent(sender =  pri.sender,
                    type = pri.type,
                    state_key = pri.state_key,
                    content = content)
        }
    }
}

@Serializable
data class LeftRoom(
        val state: Events<RawMessage>,
        val timeline: Events<RawMessage>
)

@Serializable
data class UnreadNotifications(
        val highlight_count: Int? = null,
        val notification_count: Int? = null
)
