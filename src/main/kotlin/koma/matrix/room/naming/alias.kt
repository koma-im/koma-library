package koma.matrix.room.naming

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class RoomAlias(val full: String) {
    val str: String
        get() =full
    val alias: String by lazy {
        full.substringBefore(':')
    }

    val servername: String by lazy {
        full.substringAfter(':', "<unknown server>")
    }

    override fun toString() = full

    @Serializer(forClass = RoomAlias::class)
    companion object : KSerializer<RoomAlias> {
        override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("RoomAlias", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, obj: RoomAlias) {
            encoder.encodeString(obj.full)
        }

        override fun deserialize(decoder: Decoder): RoomAlias {
            return RoomAlias(decoder.decodeString())
        }
    }
}
