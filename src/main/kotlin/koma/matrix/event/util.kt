package koma.matrix.event

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
