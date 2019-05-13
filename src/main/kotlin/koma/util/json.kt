package koma.util

import koma.matrix.json.MoshiInstance
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val adapter by lazy {
    MoshiInstance.moshi.adapter(Map::class.java).indent("    ")
}

fun formatJson(input: String): String {
    val v = try {
        adapter.fromJson(input)
    } catch (e: Exception) {
        logger.warn { "json formatter failure $e" }
        return input
    }
    val f = adapter.toJson(v)
    return f
}
