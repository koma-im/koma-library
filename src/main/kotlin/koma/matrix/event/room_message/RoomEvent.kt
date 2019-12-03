package koma.matrix.event.room_message

import koma.matrix.UserId
import koma.matrix.event.EventId
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.chat.MessageUnsigned
import koma.matrix.event.room_message.state.RoomRedactContent
import koma.matrix.json.MoshiInstance
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// try to make moshi return different kind of objects depending on a key

abstract class RoomEvent(
): Comparable<RoomEvent>{
    abstract val event_id: EventId
    abstract val origin_server_ts: Long
    abstract val type: RoomEventType

    override fun compareTo(other: RoomEvent): Int {
        return this.origin_server_ts.compareTo(other.origin_server_ts)
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        val om = other as RoomEvent
        return this.event_id == om.event_id
    }

    override fun hashCode() = this.event_id.hashCode()

    companion object {
    }
}

@Serializable
class MRoomMessage(
        //val age: Long?,
        override val  event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: JsonObject? = null,
        val sender: UserId,
        val unsigned: MessageUnsigned? = null,
        val room_id: RoomId?=null,
        /**
         * sometimes content can be as empty as {}
         */
        val content: M_Message?=null,
        override val type: RoomEventType = RoomEventType.Message
): RoomEvent() {
    override fun toString(): String {
        return "MRoomMessage(event_id=$event_id, origin_server_ts=$origin_server_ts, prev_content=$prev_content, sender=$sender, unsigned=$unsigned, room_id=$room_id, content=$content, type=$type)"
    }
}

class MRoomRedaction(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId,
        val state_key: String?,
        val redacts: String,
        val content: RoomRedactContent,
        override val type: RoomEventType = RoomEventType.Redaction
): RoomEvent()

class MRoomUnrecognized(
        //val age: Long?,
        override val event_id: EventId,
        override val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val sender: UserId?,
        val state_key: String?,
        val unsigned: MessageUnsigned?,
        val content: Map<String, Any>?,
        override val type: RoomEventType = RoomEventType.Unknown
): RoomEvent() {
    override fun toString(): String {
        return "MRoomUnrecognized(prev_content=$prev_content, sender=$sender, state_key=$state_key, unsigned=$unsigned, content=$content)"
    }
}

