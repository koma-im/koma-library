package koma.matrix.event.room_message

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.serialization.SerialName

import kotlinx.serialization.Serializable
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

    @SerialName( "m.room.guest_access") GuestAccess;

    override fun toString(): String {
        return enumToStr(this)
    }

    companion object {
        private val json = Json(JsonConfiguration.Stable.copy(unquoted = true))

        fun enumToStr(t: RoomEventType): String {
            val json = json.stringify(RoomEventType.serializer(), t)
            return json
        }
        fun strToEnum(s: String): RoomEventType? {
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
