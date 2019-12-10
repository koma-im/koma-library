package koma.matrix

import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.Serializable

/**
 * Created by developer on 2017/7/8.
 * json type of classes
 */
@Serializable
data class Chunked<T>(
        val start: String? = null,
        val end: String,
        // can be null when the transferring is done
        val chunk: List<T>
)

@Serializable
data class RoomListing(
        // this is still the total number even if returned rooms are filtered
        val total_room_count_estimate: Int,
        // probaby can be null when the transferring is done
        val next_batch: String? = null,
        val chunk: List<DiscoveredRoom>
)

@Serializable
data class DiscoveredRoom(
        val aliases: List<RoomAlias>? = null,
        val avatar_url: String? = null,
        val guest_can_join: Boolean,
        val name: String? = null,
        val num_joined_members: Int,
        val room_id: RoomId,
        val topic: String? = null,
        val world_readable: Boolean
) {
    fun dispName(): String{
        val n = name ?: aliases?.getOrNull(0)?.full ?: room_id.localstr
        return n
    }

    fun containsTerms(terms: List<String>): Boolean {
        fun String.containsAll(ss: List<String>): Boolean {
            return ss.all { this.contains(it, ignoreCase = true) }
        }
        if (this.aliases?.any {
                    // exclude the server name part, such as matrix.org
                    it.alias.containsAll(terms)
                } == true) return true
        if (this.name?.containsAll(terms) == true) return true
        if (this.topic?.containsAll(terms) == true) return true
        return false
    }
}

@Serializable
data class UploadResponse(
        val content_uri: String
)

@Serializable
data class RoomInfo(
        val room_id: RoomId)

@Serializable
class EmptyResult()

