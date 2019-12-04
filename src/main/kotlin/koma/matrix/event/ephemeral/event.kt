package koma.matrix.event.ephemeral

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * because of restrictions of moshi, sometimes we have to deal with Map
 * manually and convert it to a nice type
 */
@Serializable
data class EphemeralRawEvent(
        val type: EphemeralRawEventType,
        val content: JsonObject
)

@Serializable
enum class EphemeralRawEventType {
    @SerialName("m.typing") Typing,
    @SerialName("m.receipt") Receipt
}

sealed class EphemeralEvent()

@Serializable
data class TypingEvent(val user_ids: List<String>): EphemeralEvent()

fun EphemeralRawEvent.parse(): EphemeralEvent? {
    when(this.type) {
        EphemeralRawEventType.Typing -> {
            val userIds = this.content["user_ids"]
            if (userIds != null && userIds is List<*>) {
                val usersTyping: List<String> = userIds.map { it.toString() }
                return TypingEvent(usersTyping)
            } else {
                return null
            }
        }
        else -> {
            return null
        }
    }
}
