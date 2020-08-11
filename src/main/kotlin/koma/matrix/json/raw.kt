package koma.matrix.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

typealias RawJson<T> = Preserved<T>

class Preserved<T>(
        val raw: JsonObject,
        val value: T
) {
    fun stringify(): String {
        return jsonDefault.encodeToString(JsonObjectSerializer, raw)
    }
    override fun toString(): String {
        return "Preserved(value=$value)"
    }
}

@Serializer(forClass = Preserved::class)
internal class RawSerializer<T>(private val dataSerializer: KSerializer<T>): KSerializer<Preserved<T>> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Preserved", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Preserved<T>) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json, not $encoder")
        output.encodeJsonElement(value.raw)
    }

    override fun deserialize(decoder: Decoder): Preserved<T> {
        val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val data = input.json.decodeFromJsonElement(dataSerializer, tree)
        return Preserved(tree, data)
    }
}