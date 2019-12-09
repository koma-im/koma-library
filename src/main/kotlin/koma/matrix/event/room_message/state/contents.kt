package koma.matrix.event.room_message.state

import koma.matrix.UserId
import koma.matrix.event.room_message.chat.ImageInfo
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RoomAliasesContent(
        val aliases: List<RoomAlias>
)

@Serializable
data class RoomCanonAliasContent(
        // empty when it's a delete event
        val alias: RoomAlias? = null
)

@Serializable
class RoomHistoryVisibilityContent(
        val history_visibility: HistoryVisibility
)

@Serializable
class RoomPowerLevelsContent(
        val users_default: Float = 0.0f,
        /**
         *  state_default Defaults to 50 if unspecified,
         * but 0 if there is no m.room.power_levels event at all.
         */
        val state_default: Float = 50.0f,
        val events_default: Float = 0.0f,
        val ban: Float = 50f,
        val invite: Float = 50f,
        val kick: Float = 50f,
        val redact: Float = 50f,
        val events: Map<String, Float>,
        val users: Map<UserId, Float>
)

@Serializable
class RoomJoinRulesContent(
        val join_rule: RoomJoinRules
)

@Serializable
class RoomRedactContent(
        val reason: String
)

@Serializable
class RoomCreateContent(
        val creator: UserId,
        @SerialName("m.federate")
        val federate: Boolean = true
)

@Serializable
class RoomNameContent(
        val name: String? = null
)

@Serializable
class RoomTopicContent(
        val topic: String
)

@Serializable
class RoomAvatarContent(
        val url: String,
        val info: ImageInfo?=null
)

@Serializable
class RoomPinnedEventsContent(
        val pinned: List<String>
)
