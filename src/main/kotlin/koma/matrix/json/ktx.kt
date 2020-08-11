package koma.matrix.json

import koma.InvalidData
import koma.KomaFailure
import koma.util.KResult
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse
import kotlinx.serialization.stringify

internal val jsonDefaultConf = Json {
    ignoreUnknownKeys = true
}

internal val jsonDefault = jsonDefaultConf
internal val jsonPretty = Json(from =  jsonDefault){
    prettyPrint = true
    prettyPrintIndent = "  "
}


fun <T> Json.stringifyResult(s: SerializationStrategy<T>, obj: T): KResult<String, KomaFailure> {
    return try {
        KResult.success(this.encodeToString(s, obj))
    } catch (e: Exception) {
        KResult.failure(InvalidData("$e $obj"))
    }
}

fun <T> Json.parseResult(s: DeserializationStrategy<T>, input: String): KResult<T, KomaFailure> {
    return try {
        KResult.success(this.decodeFromString(s, input))
    } catch (e: Exception) {
        KResult.failure(InvalidData("Invalid($input)", cause = e))
    }
}

fun <T> stringify(s: SerializationStrategy<T>, obj: T): KResult<String, KomaFailure> {
    return jsonDefault.stringifyResult(s, obj)
}