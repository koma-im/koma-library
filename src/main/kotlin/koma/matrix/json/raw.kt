package koma.matrix.json

import kotlinx.serialization.*
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.json.JsonOutput
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

typealias RawJson<T> = Preserved<T>

class Preserved<T>(
        val raw: JsonObject,
        val value: T
) {
    fun stringify(): String {
        return jsonDefault.stringify(JsonObjectSerializer, raw)
    }
    override fun toString(): String {
        return "Preserved(value=$value)"
    }
}

@Serializer(forClass = Preserved::class)
internal class RawSerializer<T>(private val dataSerializer: KSerializer<T>): KSerializer<Preserved<T>> {
    override val descriptor: SerialDescriptor =
            PrimitiveDescriptor("Preserved", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Preserved<T>) {
        val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
        output.encodeJson(value.raw)
    }

    override fun deserialize(decoder: Decoder): Preserved<T> {
        val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val data = input.json.fromJson(dataSerializer, tree)
        return Preserved(tree, data)
    }
}