package koma.matrix.event.room_message

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

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

    override fun toString() = toName()

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
    }
}