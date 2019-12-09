package koma.matrix.room.naming

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class RoomId(val full: String): Comparable<RoomId> {
    val id: String
        get() =full
    val str: String
        get() =full
    val localstr by lazy {
        id.substringAfter('!').substringBefore(':')
    }

    val servername by lazy {
        id.substringAfter(':')
    }

    constructor(serv: String, local: String): this("!$local:$serv") {
    }

    override fun compareTo(other: RoomId): Int = this.id.compareTo(other.id)

    override fun toString() = full

    @Serializer(forClass = RoomId::class)
    companion object : KSerializer<RoomId> {
        override val descriptor: SerialDescriptor =
                StringDescriptor.withName("RoomId")

        override fun serialize(encoder: Encoder, obj: RoomId) {
            encoder.encodeString(obj.full)
        }

        override fun deserialize(decoder: Decoder): RoomId {
            return RoomId(decoder.decodeString())
        }
    }
}
