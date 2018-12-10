package controller

import koma.controller.sync.startSyncing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import matrix.ApiClient

/**
 * Created by developer on 2017/6/22.
 */
class ChatController(
        val apiClient: ApiClient) {

    private val shutdownSignalChan = Channel<Unit>()

    init{

    }

    fun start() {
        val start = apiClient.next_batch
        val syncEventChannel = startSyncing(start, shutdownSignalChan)
    }

    fun shutdown() {
        runBlocking {
            shutdownSignalChan.send(Unit)
            shutdownSignalChan.receive()
        }
    }
}
