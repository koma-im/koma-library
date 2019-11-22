package koma.matrix.json

import koma.InvalidData
import koma.KomaFailure
import koma.util.KResult
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify

private val jsonConf = JsonConfiguration.Stable.copy(
        strictMode = false // allow unknown keys
)

internal val jsonDefault = Json(jsonConf)


fun <T> Json.stringifyResult(s: SerializationStrategy<T>, obj: T): KResult<String, KomaFailure> {
    return try {
        KResult.success(this.stringify(s, obj))
    } catch (e: Exception) {
        KResult.failure(InvalidData("$e $obj"))
    }
}

fun <T> stringify(s: SerializationStrategy<T>, obj: T): KResult<String, KomaFailure> {
    return jsonDefault.stringifyResult(s, obj)
}