package koma.matrix

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import koma.matrix.json.NewTypeString
import koma.util.KSerializable
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@KSerializable
data class UserId(private val input: String): Comparable<UserId> {

    val user: String
    val server: String

    @Deprecated("use full")
    val str: String
        get() = input

    val full: String
        get() = input
    init {
        val s = input.trimStart('@')
        user = s.substringBefore(':')
        server = s.substringAfter(':')
    }

    override fun compareTo(other: UserId): Int = this.str.compareTo(other.str)

    @Serializer(forClass = UserId::class)
    companion object : KSerializer<UserId> {
        override val descriptor: SerialDescriptor =
                StringDescriptor.withName("UserId")

        override fun serialize(encoder: Encoder, obj: UserId) {
            encoder.encodeString(obj.input)
        }

        override fun deserialize(decoder: Decoder): UserId {
            return UserId(decoder.decodeString())
        }
    }
}

internal class UserIdAdapter {
    @ToJson
    fun toJson(value: UserId): String = value.full

    @FromJson
    fun fromJson(json: String) = UserId(json)
}

internal val userIdAdapter = UserIdAdapter()