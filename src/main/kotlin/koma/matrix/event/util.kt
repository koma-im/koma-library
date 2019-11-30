package koma.matrix.event

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class EventId(val full: String) {
    val str: String
        get() =full
    override fun toString() = full
    
     @Serializer(forClass = EventId::class)
    companion object : KSerializer<EventId> {
        override val descriptor: SerialDescriptor =
                StringDescriptor.withName("EventId")

        override fun serialize(encoder: Encoder, obj: EventId) {
            encoder.encodeString(obj.full)
        }

        override fun deserialize(decoder: Decoder): EventId {
            return EventId(decoder.decodeString())
        }
    }
}

internal class EventIdAdapter {
    @ToJson
    fun toJson(value: EventId): String = value.full

    @FromJson
    fun fromJson(json: String) = EventId(json)
}
