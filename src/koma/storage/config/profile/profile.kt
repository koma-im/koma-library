package koma.storage.config.profile

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.matrix.json.NewTypeStringAdapterFactory
import koma.matrix.room.naming.RoomId
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

val userProfileFilename = "profile.json"

class Profile(
        val userId: UserId,
        val access_token: String
) {
    companion object {
        fun new(userId: UserId): Profile?{
            val token = getToken(userId)
            return token?.access_token?.let { Profile(userId, it) }
        }
    }
}

class SavedUserState (
    val joinedRooms: List<RoomId>
)

private fun loadUserState(userId: UserId): SavedUserState? {
    val dir = userProfileDir(userId)
    dir?:return null
    val file = File(dir).resolve(userProfileFilename)
    val jsonAdapter = Moshi.Builder()
            .add(NewTypeStringAdapterFactory())
            .build()
            .adapter(SavedUserState::class.java)
    val savedRoomState = try {
        jsonAdapter.fromJson(file.readText())
    } catch (e: FileNotFoundException) {
        println("$file not found")
        return null
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
    return savedRoomState
}
