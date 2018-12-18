package koma_app

import koma.storage.config.server.ServerConf
import matrix.ApiClient

object appState {
    lateinit var apiClient: ApiClient
    lateinit var serverConf: ServerConf

    init {
    }
}


