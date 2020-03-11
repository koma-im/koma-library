package koma.matrix.json

import koma.InvalidData
import koma.KomaFailure
import koma.util.KResult
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify

internal val jsonDefaultConf = JsonConfiguration.Stable.copy(
        ignoreUnknownKeys = true
)

internal val jsonDefault = Json(jsonDefaultConf)
internal val jsonPretty = Json(jsonDefaultConf.copy(prettyPrint = true, indent = "  "))


fun <T> Json.stringifyResult(s: SerializationStrategy<T>, obj: T): KResult<String, KomaFailure> {
    return try {
        KResult.success(this.stringify(s, obj))
    } catch (e: Exception) {
        KResult.failure(InvalidData("$e $obj"))
    }
}

fun <T> Json.parseResult(s: DeserializationStrategy<T>, input: String): KResult<T, KomaFailure> {
    return try {
        KResult.success(this.parse(s, input))
    } catch (e: Exception) {
        KResult.failure(InvalidData("Invalid($input)", cause = e))
    }
}
fun <T> stringify(s: SerializationStrategy<T>, obj: T): KResult<String, KomaFailure> {
    return jsonDefault.stringifyResult(s, obj)
}