package koma.util

import koma.matrix.json.MoshiInstance
import mu.KotlinLogging

import kotlinx.serialization.Serializable

internal typealias KSerializable = Serializable

private val logger = KotlinLogging.logger {}

fun formatJson(input: String): String {
    val adapter = MoshiInstance.mapAdapterIndented
    val v = try {
        adapter.fromJson(input)
    } catch (e: Exception) {
        logger.warn { "json formatter failure $e" }
        return input
    }
    val f = adapter.toJson(v)
    return f
}
