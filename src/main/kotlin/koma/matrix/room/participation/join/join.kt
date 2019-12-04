package koma.matrix.room.participation.join

import koma.matrix.room.naming.RoomId
import kotlinx.serialization.Serializable

@Serializable
data class JoinRoomResult(val room_id: RoomId)

