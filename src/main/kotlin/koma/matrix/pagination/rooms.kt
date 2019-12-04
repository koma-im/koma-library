package koma.matrix.pagination

import kotlinx.serialization.Serializable

@Serializable
data class RoomBatch<T>(
        // this is still the total number even if returned rooms are filtered
        val total_room_count_estimate: Int,
        // probaby can be null when the transferring is done
        val next_batch: String? = null,
        val chunk: List<T>
)

