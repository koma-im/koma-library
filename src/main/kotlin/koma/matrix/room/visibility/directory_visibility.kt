package koma.matrix.room.visibility

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RoomVisibility {
    @SerialName("public")
    Public,
    @SerialName( "private")
    Private;

    override fun toString(): String {
        return when (this) {
            RoomVisibility.Public -> "public"
            RoomVisibility.Private -> "private"
        }
    }
}
