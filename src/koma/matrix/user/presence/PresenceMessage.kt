package koma.matrix.user.presence

import com.squareup.moshi.Json
import koma.matrix.UserId

enum class PresenceEventType {
    @Json(name = "m.presence") Presence
}

data class PresenceMessage(
        val sender:UserId,
        val type: PresenceEventType,
        val content: PresenceMessageContent) {
}

data class PresenceMessageContent (
    val avatar_url: String? = null,
    val displayname: String? = null,
    val last_active_ago: Long?,
    val presence: UserPresenceType,
    val user_id: UserId?
    )

enum class UserPresenceType {
    @Json(name="online") Online,
    @Json(name="offline") Offline,
    @Json(name="unavailable") Unavailable,
}
