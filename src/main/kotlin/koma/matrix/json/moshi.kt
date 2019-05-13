package koma.matrix.json

import com.squareup.moshi.Moshi
import koma.matrix.event.rooRoomEvent.RoomEventAdapterFactory
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.chat.MessageAdapterFactory
import koma.matrix.event.room_message.RoomEventTypeEnumAdapter
import koma.matrix.event.stripped.InviteEventAdapterFactory

object MoshiInstance{
    val moshiBuilder = Moshi.Builder()
            .add(MessageAdapterFactory())
            .add(InviteEventAdapterFactory())
            .add(RoomEventAdapterFactory())
            .add(NewTypeStringAdapterFactory())
            .add(RoomEventTypeEnumAdapter())
    val moshi = moshiBuilder.build()
    val roomEventAdapter = moshi.adapter(RoomEvent::class.java)
    init {
    }
}
