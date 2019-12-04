package koma.matrix.event.context

import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.Preserved
import koma.matrix.json.RawJson
import koma.matrix.json.RawSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ContextResponse(
        /**
         *A token that can be used to paginate backwards with.
         * Actually, I'm afraid it can be null
         */
        val start: String,
        val end: String,
        val events_before: List<@Serializable(with=RawSerializer::class) Preserved<RoomEvent>>,
        val event: @Serializable(with=RawSerializer::class) Preserved<RoomEvent>,
        val events_after: List<@Serializable(with=RawSerializer::class) Preserved<RoomEvent>>,
        val state: List<JsonObject>
)
