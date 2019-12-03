package koma.matrix.event.room_message.state

import koma.matrix.UserId
import koma.matrix.event.EventId
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.member.PrevContent
import koma.matrix.event.room_message.state.member.RoomMemberContent
import koma.matrix.event.room_message.state.member.RoomMemberUnsigned
import koma.matrix.event.room_message.state.member.StrippedState
import koma.matrix.event.room_message.RoomEventType

sealed class RoomStateEvent: RoomEvent() {
    abstract override val event_id: EventId
    abstract override val origin_server_ts: Long
    abstract override val type: RoomEventType
}
class MRoomAliases(
        //val age: Long?,
        override val origin_server_ts: Long,
        override val event_id: EventId,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String,
        val content: RoomAliasesContent,
        override val type: RoomEventType = RoomEventType.Aliases
): RoomStateEvent()

class MRoomCanonAlias(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomCanonAliasContent,
        override val type: RoomEventType = RoomEventType.CanonAlias
): RoomStateEvent()

class MRoomCreate(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomCreateContent,
        override val type: RoomEventType = RoomEventType.Create
): RoomStateEvent()

class MRoomJoinRule(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomJoinRulesContent,
        override val type: RoomEventType = RoomEventType.JoinRule
): RoomStateEvent()

class MRoomMember(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: PrevContent?,
        val sender: UserId,
        val unsigned: RoomMemberUnsigned?,
        val replaces_state: String?,
        val state_key: String?,
        val invite_room_state: List<StrippedState>?,
        val content: RoomMemberContent,
        override val type: RoomEventType = RoomEventType.Member
): RoomStateEvent()

class MRoomPowerLevels(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val content: RoomPowerLevelsContent,
        override val type: RoomEventType = RoomEventType.PowerLevels
): RoomStateEvent()

class MRoomPinnedEvents(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomPinnedEventsContent,
        override val type: RoomEventType = RoomEventType.PinnedEvents
): RoomStateEvent()


class MRoomTopic(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomTopicContent,
        override val type: RoomEventType = RoomEventType.Topic
): RoomStateEvent()

class MRoomName(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomNameContent,
        override val type: RoomEventType = RoomEventType.Name
): RoomStateEvent()

class MRoomAvatar(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomAvatarContent,
        override val type: RoomEventType = RoomEventType.Avatar
): RoomStateEvent()

class MRoomHistoryVisibility(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: RoomHistoryVisibilityContent,
        override val type: RoomEventType = RoomEventType.HistoryVisibility
): RoomStateEvent()

class MRoomGuestAccess(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: Content,
        override val type: RoomEventType = RoomEventType.GuestAccess
): RoomStateEvent() {
    data class Content(
            val guest_access: GuestAccess
    ) {
        enum class GuestAccess{
            can_join,
            forbidden
        }
    }
}

