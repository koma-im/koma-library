package koma.matrix.room.participation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RoomJoinRules {
    @SerialName("public")
    Public,
    @SerialName("knock")
    Knock,
    @SerialName("invite")
    Invite,
    @SerialName("private")
    Private;

    companion object {
        fun fromString(rule: String): RoomJoinRules? {
            val join = when (rule) {
                "public" -> RoomJoinRules.Public
                "invite" -> RoomJoinRules.Invite
            // not used on the matrix network for now
                "knock" -> RoomJoinRules.Knock
                "private" -> RoomJoinRules.Private
                else -> null
            }
            return join
        }
    }
}
