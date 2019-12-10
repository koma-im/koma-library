package koma.matrix.sync

import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * message received as a dict from the server
 */
@Serializable
data class RawMessage(
        val age: Long? = null,
        val event_id: String,
        val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val type: RoomEventType,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: JsonObject) {
}

