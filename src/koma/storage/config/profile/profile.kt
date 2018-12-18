package koma.storage.config.profile

import koma.matrix.UserId
import koma.matrix.room.naming.RoomId

val userProfileFilename = "profile.json"

class Profile(
        val userId: UserId,
        val access_token: String
)

class SavedUserState (
    val joinedRooms: List<RoomId>
)
