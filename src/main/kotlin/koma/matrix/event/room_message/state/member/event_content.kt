package koma.matrix.event.room_message.state.member

import koma.matrix.UserId
import koma.matrix.room.participation.Membership
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class RoomMemberContent(
        val membership: Membership,
        val avatar_url: String?,
        val displayname: String? = null,
        val is_direct: Boolean? = null,
        val third_party_invite: Invite? = null,
        val inviter: UserId? = null
)

@Serializable
class PrevContent(
        val avatar_url: String? = null,
        val membership: Membership? = null,
        val is_direct: Boolean? = null,
        val displayname: String? = null
)

@Serializable
class RoomMemberUnsigned(
        val age: Long,
        val prev_content: PrevContent? = null,
        val prev_sender: UserId? = null,
        val replaces_state: String? = null
)

@Serializable
class Invite(
        val displayname: String
        /**
         * signed should be ignored by clients
         */
        // val signed
)

/**
 * m.room.member may also include an invite_room_state key outside
 * the content key. If present, this contains an array of StrippedState Events
 */
@Serializable
class StrippedState(
        val state_key: String,
        val type: String,
        val content: JsonObject
)

