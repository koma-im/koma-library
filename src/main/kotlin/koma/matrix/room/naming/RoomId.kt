package koma.matrix.room.naming

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import koma.matrix.UserId
import koma.matrix.json.NewTypeString
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

fun canBeValidRoomId(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('!') && ss[1].isNotEmpty()
}


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

internal class RoomIdAdapter {
    @ToJson
    fun toJson(value: RoomId): String = value.full

    @FromJson
    fun fromJson(json: String) = RoomId(json)
}
