package koma.matrix.json

import com.squareup.moshi.*
import mu.KotlinLogging
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

private val logger = KotlinLogging.logger {}

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
