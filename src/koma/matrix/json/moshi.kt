package koma.matrix.json

import com.squareup.moshi.Moshi
import koma.matrix.event.rooRoomEvent.RoomEventAdapterFactory
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.chat.MessageAdapterFactory
import matrix.event.room_message.RoomEventTypeEnumAdapter

object MoshiInstance{
    val moshiBuilder = Moshi.Builder()
            .add(MessageAdapterFactory())
            .add(RoomEventAdapterFactory())
            .add(NewTypeStringAdapterFactory())
            .add(RoomEventTypeEnumAdapter())
    val moshi = moshiBuilder.build()
    val roomEventAdapter = moshi.adapter(RoomEvent::class.java)
    init {
    }
}
