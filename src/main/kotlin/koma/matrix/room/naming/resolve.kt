package koma.matrix.room.naming

import kotlinx.serialization.Serializable

@Serializable
data class ResolveRoomAliasResult(
        val room_id: RoomId,
        val servers: List<String>
)
