package koma.matrix.user.presence

import koma.matrix.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
enum class PresenceEventType {
    @SerialName("m.presence")
    Presence
}

@Serializable
data class PresenceMessage(
        val sender:UserId,
        val type: PresenceEventType,
        val content: PresenceMessageContent) {
}

@Serializable
data class PresenceMessageContent (
    val avatar_url: String? = null,
    val displayname: String? = null,
    val last_active_ago: Long? = null,
    val presence: UserPresenceType,
    val user_id: UserId? = null
    )

@Serializable
enum class UserPresenceType {
    @SerialName("online")
    Online,
    @SerialName("offline")
    Offline,
    @SerialName("unavailable")
    Unavailable,
}
