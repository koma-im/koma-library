package koma.matrix.room.admin

import koma.matrix.UserId
import kotlinx.serialization.Serializable

@Serializable
data class MemberBanishment(val user_id: UserId)

@Serializable
class BanRoomResult()
