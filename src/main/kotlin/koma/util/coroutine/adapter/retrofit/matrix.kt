package koma.util.coroutine.adapter.retrofit

import io.ktor.http.HttpStatusCode
import koma.MatrixFailure
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.primitive

internal fun JsonObject.toMatrixFailure(statusCode: HttpStatusCode,
                                        http_body: String
): MatrixFailure? {
    val content = this.toMutableMap()
    val m = this.mapValuesTo(LinkedHashMap()) {
        kotlin.runCatching { it.value.jsonPrimitive }.getOrNull()
    }
    val c = content.remove("errcode")?.let { it.jsonPrimitive.contentOrNull } ?: return null
    val e = content.remove("error")?.let { it.jsonPrimitive.contentOrNull } ?: return null
    return MatrixFailure(c, e, content, statusCode, http_body)
}