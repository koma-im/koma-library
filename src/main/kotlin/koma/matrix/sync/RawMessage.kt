package koma.matrix.sync

import com.squareup.moshi.Moshi
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.json.MoshiInstance
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * message received as a dict from the server
 */
@Serializable
data class RawMessage(
        val age: Long?,
        val event_id: String,
        val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val type: RoomEventType,
        val sender: UserId,
        val state_key: String? = null,
        val txn_id: String? = null,
        val content: JsonObject) {
    companion object {
        private val adapter = MoshiInstance.moshi.adapter<RawMessage>(RawMessage::class.java)
    }

    /**
     * convert to string to storage
     * omit age, which is temporary
     */
    fun toJson(): String{
        return adapter.toJson(this.copy(age=null))
    }
}

