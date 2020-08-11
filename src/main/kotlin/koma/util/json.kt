package koma.util


import koma.matrix.json.jsonDefaultConf
import koma.matrix.json.jsonPretty
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElementSerializer

private val logger = KotlinLogging.logger {}

fun formatJson(input: String): String {
    try {
        val element = jsonPretty.decodeFromString(JsonElementSerializer, input)
        return jsonPretty.encodeToString(JsonElementSerializer, element)
    } catch (e: Exception) {
        logger.warn { "json formatter failure $e" }
        return input
    }
}
