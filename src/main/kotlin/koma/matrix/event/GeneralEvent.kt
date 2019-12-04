package koma.matrix.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeneralEvent (
        val type: String,
        val content: JsonObject
)

