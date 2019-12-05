package koma.matrix.event.room_message

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import koma.matrix.room.naming.RoomAlias
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
enum class RoomEventType{
    @SerialName( "m.room.aliases") Aliases,
    @SerialName( "m.room.canonical_alias") CanonAlias,
    @SerialName( "m.room.create") Create,
    @SerialName( "m.room.join_rules") JoinRule,
    @SerialName( "m.room.power_levels") PowerLevels,
    @SerialName( "m.room.member") Member,
    @SerialName( "m.room.message") Message,
    @SerialName( "m.room.redaction") Redaction,

    @SerialName( "m.room.name") Name,
    @SerialName( "m.room.topic") Topic,
    @SerialName( "m.room.avatar") Avatar,
    PinnedEvents,
    @SerialName( "m.room.bot.options") BotOptions,

    @SerialName( "m.room.history_visibility") HistoryVisibility,

    @SerialName( "m.room.guest_access") GuestAccess,
    Unknown;

    override fun toString(): String {
        return enumToStr(this)
    }

    fun toName(): String = enumStringMap[this].toString()
    
    @Serializer(forClass = RoomEventType::class)
    companion object  : KSerializer<RoomEventType>  {
        private val stringEnumMap = mapOf(
                "m.room.aliases" to Aliases,
                "m.room.canonical_alias" to CanonAlias,
                "m.room.create" to Create,
                "m.room.join_rules" to JoinRule,
                "m.room.power_levels" to PowerLevels,
                "m.room.member" to Member,
                "m.room.message" to Message,
                "m.room.redaction" to Redaction,
                "m.room.name" to Name,
                "m.room.topic" to Topic,
                "m.room.avatar" to Avatar,
                "m.room.bot.options" to BotOptions,
                "m.room.history_visibility" to HistoryVisibility,
                "m.room.guest_access" to GuestAccess
        )
        private val enumStringMap = stringEnumMap.entries.associate { it.value to it.key }
        
        override val descriptor: SerialDescriptor =
                StringDescriptor.withName("RoomEventType")
        override fun serialize(encoder: Encoder, obj: RoomEventType) {
            val s = enumStringMap[obj]
            encoder.encodeString(s ?: obj.name)
        }

        override fun deserialize(decoder: Decoder): RoomEventType {
            val s = decoder.decodeString()
            return stringEnumMap[s] ?: RoomEventType.Unknown
        }
        private val json = Json(JsonConfiguration.Stable.copy(unquoted = true))

        internal fun enumToStr(t: RoomEventType): String {
            val json = json.stringify(RoomEventType.serializer(), t)
            return json
        }
        internal fun strToEnum(s: String): RoomEventType? {
            return try {
                json.parse(serializer(), s)
            } catch (e: Exception) {
                null
            }
        }
    }
}

internal class RoomEventTypeEnumAdapter {
    companion object {
        private val json = Json(JsonConfiguration.Stable)
    }

    @ToJson
    fun toJson(t: RoomEventType) = json.stringify(RoomEventType.serializer(), t)

    @FromJson
    fun fromJson(str: String): RoomEventType? {
        return try {
            json.parse(RoomEventType.serializer(), str)
        } catch (e: Exception) {
            null
        }
    }
}
