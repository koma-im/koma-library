package koma.matrix.publicapi.rooms

import koma.HttpFailure
import koma.Server
import koma.matrix.DiscoveredRoom
import koma.matrix.MatrixApi
import koma.util.failureOrThrow
import koma.util.getOrThrow
import koma.util.testFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
fun CoroutineScope.getPublicRooms(server: Server) = produce<DiscoveredRoom>(capacity = 1) {
    var since: String? = null
    var fetched = 0
    while (true) {
        val (roomBatch, failure, result) = server.listPublicRooms(since)
        if (!result.testFailure(roomBatch, failure)) {
            val rooms = roomBatch.chunk
            fetched += rooms.size
            logger.debug { "Fetched ${rooms.size} rooms ($fetched/${roomBatch.total_room_count_estimate})" }
            rooms.forEach { send(it) }
            val next = roomBatch.next_batch
            if (next == null || next == since) {
                println("Finished fetching public rooms $fetched in total")
                close()
                return@produce
            }
            since = next
        } else {
            logger.error { "Error fetching public rooms $failure" }
            delay(1000)
        }
    }
}

@ExperimentalCoroutinesApi
fun CoroutineScope.findPublicRooms(term: String, service: MatrixApi) = produce() {
    var since: String? = null
    var fetched = 0
    while (true) {
        val call_res = service.findPublicRooms(
                RoomDirectoryQuery(RoomDirectoryFilter(term), since = since)
                )
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
            if (error is HttpFailure) {
                logger.error { "Http Error $error finding public rooms with $term" }
                close()
                return@produce
            }
            println("Error finding public rooms with $term: $error")
            delay(1000)
        }
    }
}
