package koma.matrix

import koma.matrix.event.room_message.RoomEvent
import koma.matrix.json.Preserved
import koma.matrix.json.RawSerializer
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.*

/**
 * Created by developer on 2017/7/8.
 * json type of classes
 */

@Serializable
data class MessageChunks(
        val start: String? = null,
        val end: String,
        val chunk: List<@Serializable(with = RawSerializer::class) Preserved<RoomEvent>> = listOf()
)

@Serializable
data class RoomListing(
        // this is still the total number even if returned rooms are filtered
        val total_room_count_estimate: Int,
        val next_batch: String? = null,
        val chunk: List<DiscoveredRoom>
)

@Serializable
data class RegistrationData(
        val password: String,
        val username: String? = null,
        val auth: AuthenticationData? = null,
        val device_id: String? = null,
        val initial_device_display_name: String? = null
) {
    @Serializer(forClass = RegistrationData::class)
    companion object : KSerializer<RegistrationData> {
        override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("RegistrationData", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, obj: RegistrationData) {
            val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
            val m = mutableMapOf<String, JsonElement>(
                    "password" to JsonPrimitive(obj.password)
            )
            obj.username?.let { m["username"] = JsonPrimitive(it) }
            obj.device_id?.let { m["device_id"] = JsonPrimitive(it) }
            obj.initial_device_display_name?.let { m["initial_device_display_name"] = JsonPrimitive(it) }
            if (obj.auth != null) {
                val j = output.json.encodeToJsonElement(AuthenticationData.serializer(), obj.auth)
                m["auth"] = j
            }
            output.encodeJsonElement(JsonObject(m))
        }
    }
}

@Serializable
data class AuthenticationData(
        val type: String,
        val session: String ? = null
)


@Serializable
data class RegistrationResponse(
        val user_id: UserId,
        /**
         * Required if the inhibit_login option is false.
         */
        val access_token: String? = null,
        val device_id: String? = null
)

@Serializable
data class DiscoveredRoom(
        val aliases: List<RoomAlias>? = null,
        val avatar_url: String? = null,
        val guest_can_join: Boolean,
        val name: String? = null,
        val num_joined_members: Int,
        val room_id: RoomId,
        val topic: String? = null,
        val world_readable: Boolean
) {
    fun dispName(): String{
        val n = name ?: aliases?.getOrNull(0)?.full ?: room_id.localstr
        return n
    }

    fun containsTerms(terms: List<String>): Boolean {
        fun String.containsAll(ss: List<String>): Boolean {
            return ss.all { this.contains(it, ignoreCase = true) }
        }
        if (this.aliases?.any {
                    // exclude the server name part, such as matrix.org
                    it.alias.containsAll(terms)
                } == true) return true
        if (this.name?.containsAll(terms) == true) return true
        if (this.topic?.containsAll(terms) == true) return true
        return false
    }
}

@Serializable
data class UploadResponse(
        val content_uri: String
)

@Serializable
data class RoomInfo(
        val room_id: RoomId)

@Serializable
class EmptyResult()

