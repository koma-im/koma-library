package koma.matrix.room.participation.invite

import koma.matrix.UserId
import kotlinx.serialization.Serializable

@Serializable
data class InviteUserData(val user_id: UserId)

@Serializable
class InviteMemResult()
