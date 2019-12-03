package koma.matrix.json

import com.squareup.moshi.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonOutput
import mu.KotlinLogging
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

private val logger = KotlinLogging.logger {}

class Preserved<T>(
        val raw: JsonObject,
        val value: T
) {
    override fun toString(): String {
        return "Preserved(value=$value)"
    }

}

@Serializer(forClass = Preserved::class)
internal class RawSerializer<T>(private val dataSerializer: KSerializer<T>): KSerializer<Preserved<T>> {
    override val descriptor: SerialDescriptor =
            StringDescriptor.withName("RawJsonS")

    override fun serialize(encoder: Encoder, obj: Preserved<T>) {
        val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
        output.encodeJson(obj.raw)
    }

    override fun deserialize(decoder: Decoder): Preserved<T> {
        val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val data = input.json.fromJson(dataSerializer, tree)
        return Preserved(tree, data)
    }
}

data class RawJson<T>(
        val raw: Map<*, *>,
        val value: T
)

internal class RawJsonAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<RawJson<Any?>>? {
        if (annotations.isNotEmpty()) {
            return null
        }
        if (type !is ParameterizedType) {
            return null
        }
        if (Types.getRawType(type) != RawJson::class.java) {
            return null
        }
        val par = type.actualTypeArguments.firstOrNull()
        if (par == null) {
            logger.error { "no actualTypeArgument" }
            return null
        }
        val c = Types.getRawType(par)
        return RawJsonAdapter(moshi, c)
    }
}

private class RawJsonAdapter(m: Moshi, c: Class<out Any>): JsonAdapter<RawJson<Any?>>() {
    private val adapter = m.adapter<Any?>(c)

    override fun toJson(writer: JsonWriter, data: RawJson<Any?>?) {
        data ?: return
        adapter.toJson(writer, data.value)
    }

    override fun fromJson(jr: JsonReader): RawJson<Any?>? {
        val raw = jr.peekJson().readJsonValue()
        if (raw !is Map<*, *>) {
            logger.error { "$raw not map" }
            return null
        }
        val v =  adapter.fromJson(jr)
        return RawJson(raw, v)
    }
}
