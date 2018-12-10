package koma.storage.rooms.state

import koma.storage.config.config_paths
import java.io.File
import java.util.stream.Stream

val state_dir = config_paths.getCreateDir("state")

fun load_members(file: File): Stream<Pair<String, Float?>>
        = file.bufferedReader().lines().map {
        val l = it.split(' ', limit = 2)
        val user = l[0]
        val lvl = l.getOrNull(1)?.toFloatOrNull()
        user to lvl
    }
