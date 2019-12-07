package koma.matrix.sync

import koma.matrix.json.jsonDefault
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class SyncResponseTest {

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun test1() {
        val syncResponse = SyncResponse("test1nb",
                Events(listOf()), Events(listOf()),
                RoomsResponse(mapOf(), mapOf(), mapOf())
                )
        val srText = jsonDefault.stringify(SyncResponse.serializer(), syncResponse)
        val deserilizedSync = jsonDefault.parse(SyncResponse.serializer(), srText)
    }
}