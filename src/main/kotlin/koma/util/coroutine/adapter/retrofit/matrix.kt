package koma.util.coroutine.adapter.retrofit

import koma.*
import koma.matrix.json.jsonDefault
import koma.matrix.json.parseResult
import koma.util.testFailure
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun tryGetMatrixFailure(s: String, code: Int, message: String): MatrixFailure? {
    val (m, e, result) = jsonDefault.parseResult(JsonObject.serializer(), s)
    if (result.testFailure(m ,e)) {
        logger.warn { "Is not json, $s. Exception: $e" }
        return null
    }
    return m.toMatrixFailure(code, message)
}

internal fun JsonObject.toMatrixFailure(code: Int, message: String): MatrixFailure? {
    val j: MutableMap<String, Any> = this.toMutableMap()
    val c = j.remove("errcode")?.toString() ?: return null
    val e = j.remove("error")?.toString() ?: return null
    return MatrixFailure(c, e, j, code, message)
}