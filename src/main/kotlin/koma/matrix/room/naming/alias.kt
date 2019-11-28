package koma.matrix.room.naming

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import koma.matrix.json.NewTypeString
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

fun canBeValidRoomAlias(input: String): Boolean {
    val ss = input.split(':')
    if (ss.size != 2) return false
    return ss[0].startsWith('#') && ss[1].isNotEmpty()
}

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
                StringDescriptor.withName("RoomAlias")

        override fun serialize(encoder: Encoder, obj: RoomAlias) {
            encoder.encodeString(obj.full)
        }

        override fun deserialize(decoder: Decoder): RoomAlias {
            return RoomAlias(decoder.decodeString())
        }
    }
}

internal class RoomAliasAdapter {
    @ToJson
    fun toJson(value: RoomAlias): String = value.full

    @FromJson
    fun fromJson(json: String) = RoomAlias(json)
}
