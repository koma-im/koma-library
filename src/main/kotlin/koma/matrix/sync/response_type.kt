package koma.matrix.sync

import koma.matrix.room.naming.RoomId
import koma.matrix.user.presence.PresenceMessage
import koma.matrix.room.InvitedRoom
import koma.matrix.room.JoinedRoom
import koma.matrix.room.LeftRoom

data class Events<T>(
        val events: List<T>
)

data class SyncResponse(
        val next_batch: String,
        val presence: Events<PresenceMessage>,
        val account_data: Events<Map<String, Any>>,
        val rooms: RoomsResponse
) {
    override fun toString(): String {
        return "SyncResponse(next_batch=$next_batch,rooms=$rooms)"
    }
}

data class RoomsResponse(
        val join: Map<String, JoinedRoom>,
        val invite: Map<RoomId, InvitedRoom>,
        val leave: Map<RoomId, LeftRoom>
) {
    override fun toString(): String {
        return "RoomsResponse(${join.size} joined rooms)"
    }
}
