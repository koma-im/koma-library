package koma_app

import controller.ChatController
import koma.storage.config.server.ServerConf
import matrix.ApiClient

object appState {
    lateinit var chatController: ChatController
    var apiClient: ApiClient? = null
    lateinit var serverConf: ServerConf

    init {
    }
}


