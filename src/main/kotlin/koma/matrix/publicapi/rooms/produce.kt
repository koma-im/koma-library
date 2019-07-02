package koma.matrix.publicapi.rooms

import koma.matrix.DiscoveredRoom
import koma.matrix.MatrixApi
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.failureOrThrow
import koma.util.getOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import retrofit2.HttpException

@ExperimentalCoroutinesApi
fun getPublicRooms(client: MatrixApi) = GlobalScope.produce<DiscoveredRoom>(capacity = 1) {
    val service = client.service
    var since: String? = null
    var fetched = 0
    while (true) {
        val call_res = service.publicRooms(since).awaitMatrix()
        when {
            call_res.isSuccess -> {
                val roomBatch = call_res.getOrThrow()
                val rooms = roomBatch.chunk
                fetched += rooms.size
                println("Fetched ${rooms.size} rooms ($fetched/${roomBatch.total_room_count_estimate})")
                rooms.forEach { send(it) }
                val next = roomBatch.next_batch
                if (next == null || next == since) {
                    println("Finished fetching public rooms $fetched in total")
                    close()
                    return@produce
                }
                since = next
            }
            else -> {
                println("Error fetching public rooms")
                delay(1000)
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun findPublicRooms(term: String, service: MatrixApi) = GlobalScope.produce() {
    var since: String? = null
    var fetched = 0
    while (true) {
        val call_res = service.findPublicRooms(
                RoomDirectoryQuery(RoomDirectoryFilter(term), since = since)
                ).awaitMatrix()
        if (call_res.isSuccess) {
            val roomBatch = call_res.getOrThrow()
            val rooms = roomBatch.chunk
            fetched += rooms.size
            println("Fetched ${rooms.size} rooms match $term ($fetched/${roomBatch.total_room_count_estimate})")
            rooms.forEach { send(it) }
            val next = roomBatch.next_batch
            if (next == null || next == since) {
                println("Finished fetching public rooms matching $term $fetched in total")
                close()
                return@produce
            }
            since = next
        } else {
            val error = call_res.failureOrThrow()
            if (error is HttpException) {
                println("Http Error ${error.code()} ${error.message()} finding public rooms with $term")
                close()
                return@produce
            }
            println("Error finding public rooms with $term: $error")
            delay(1000)
        }
    }
}
