package koma.util.coroutine.adapter.retrofit

import io.ktor.http.HttpStatusCode
import koma.MatrixFailure
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull

internal fun JsonObject.toMatrixFailure(statusCode: HttpStatusCode,
                                        http_body: String
): MatrixFailure? {
    val content = this.content.toMutableMap()
    val m = this.mapValuesTo(LinkedHashMap()) {
        kotlin.runCatching { it.value.primitive }.getOrNull()
    }
    val c = content.remove("errcode")?.contentOrNull ?: return null
    val e = content.remove("error")?.contentOrNull ?: return null
    return MatrixFailure(c, e, content, statusCode, http_body)
}