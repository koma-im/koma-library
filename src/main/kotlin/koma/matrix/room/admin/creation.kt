package koma.matrix.room.admin

import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.matrix.room.visibility.RoomVisibility

import kotlinx.serialization.Serializable
@Serializable
class CreateRoomSettings(
        val room_alias_name: String,
        val visibility: RoomVisibility)

@Serializable
data class CreateRoomResult(
        val room_id: RoomId,
        val room_alias: RoomAlias? = null
)
