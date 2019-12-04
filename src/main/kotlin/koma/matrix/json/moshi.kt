package koma.matrix.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import koma.InvalidData
import koma.KomaFailure
import koma.matrix.NotificationResponse
import koma.matrix.UserIdAdapter
import koma.matrix.event.EventIdAdapter
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.RoomEventTypeEnumAdapter
import koma.matrix.event.room_message.chat.MessageAdapterFactory
import koma.matrix.room.naming.RoomAliasAdapter
import koma.matrix.room.naming.RoomIdAdapter
import koma.toFailure
import koma.util.*
import koma.util.coroutine.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import okio.BufferedSource
import kotlin.time.Duration
import kotlin.time.seconds

object MoshiInstance{
    val moshiBuilder = Moshi.Builder()
            .add(MessageAdapterFactory())
            .add(UserIdAdapter())
            .add(RoomIdAdapter())
            .add(RoomAliasAdapter())
            .add(EventIdAdapter())
            .add(RoomEventTypeEnumAdapter())
            .add(JsonElementAdapter())
            .add(RawJsonAdapterFactory())
    val moshi = moshiBuilder.build()
    val mapAdapter = moshi.adapter(Map::class.java)
    val mapAdapterIndented = mapAdapter.indent("    ")
    val roomEventAdapter = moshi.adapter(RoomEvent::class.java)
    init {
    }
}

private class JsonElementAdapter {
    @ToJson
    fun elementToJson(value: JsonElement): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(json: String): JsonElement {
        return jsonDefault.parseJson(json)
    }
}
inline fun<reified T: Any> deserialize(json: String): KResult<T, KomaFailure> {
    val adapter = MoshiInstance.moshi.adapter(T::class.java)
    return runCatch {
        adapter.fromJson(json)
    }.mapFailure {
        it.toFailure()
    }.flatMap {
        if (it != null)KResult.success<T, KomaFailure>(it) else KResult.failure(InvalidData(json))
    }
}

/**
 * it may take some time to perform IO
 * the read operation may may wait indefinitely depending on the read timeout of the http client
 */
suspend inline fun<reified T : Any> deserialize(source: BufferedSource
                                                , timeout: Duration = 30.seconds
): KResult<T, KomaFailure> {
    val adapter = MoshiInstance.moshi.adapter(T::class.java)
    val (success, failure, result) = withTimeout(timeout) {
        withContext(Dispatchers.IO) {
            runCatch {
                adapter.fromJson(source)
            }.mapFailure {
                it.toFailure()
            }.flatMap {
                if (it != null) KResult.success<T, KomaFailure>(it)
                else KResult.failure(InvalidData("not ${T::class.java.canonicalName}"))
            }
        }
    }
    if (result.testFailure(success, failure)) {
        return KResult.failure(failure)
    }
    return success
}