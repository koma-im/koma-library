package koma.matrix.pagination

import koma.matrix.DiscoveredRoom
import koma.matrix.RoomListing
import koma.matrix.json.jsonDefault
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RoomBatchTest {
    val listing = """{
  "chunk": [
    {
      "aliases": [
        "#murrays:cheese.bar"
      ],
      "avatar_url": "mxc://bleeker.street/CHEDDARandBRIE",
      "guest_can_join": false,
      "name": "CHEESE",
      "num_joined_members": 37,
      "room_id": "!ol19s:bleecker.street",
      "topic": "Tasty tasty cheese",
      "world_readable": true
    }
  ],
  "next_batch": "p190q",
  "prev_batch": "p1902",
  "total_room_count_estimate": 115
}"""
    @Test
    fun getChunk() {
        val discovery = jsonDefault.decodeFromString(RoomListing.serializer(), listing)
        assertEquals(115, discovery.total_room_count_estimate)
    }
}