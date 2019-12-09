package koma.matrix.room.visibility

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HistoryVisibility {
    @SerialName("invited")
    Invited,
    @SerialName( "joined")
    Joined,
    @SerialName("shared")
    Shared,
    @SerialName("world_readable")
    WorldReadable;

    companion object {
        internal fun fromString(hvstr: String): HistoryVisibility {
            val vis = when (hvstr) {
                "invited" -> HistoryVisibility.Invited
                "joined" -> HistoryVisibility.Joined
                "shared" -> HistoryVisibility.Shared
                "world_readable" -> HistoryVisibility.WorldReadable
                "Invited" -> HistoryVisibility.Invited
                "Joined" -> HistoryVisibility.Joined
                "Shared" -> HistoryVisibility.Shared
                "WorldReadable" -> HistoryVisibility.WorldReadable
                else -> throw Exception("$hvstr is not one of ${HistoryVisibility.values()}")
            }
            return vis
        }
    }
}
