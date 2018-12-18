package koma.storage.config.profile

import koma.Koma
import koma.matrix.UserId
import java.io.File
import java.io.IOException

fun Koma.userProfileDir(userid: UserId): String? {
    return this.paths.getCreateProfileDir(userid.server, userid.user)
}

fun Koma.saveSyncBatchToken(userid: UserId, next_batch: String) {
    val userdir = userProfileDir(userid)
    userdir?: return
    val syncTokenFile = File(userdir).resolve("next_batch")
    try {
        syncTokenFile.writeText(next_batch)
    } catch (e: IOException) {
        return
    }
}

fun Koma.loadSyncBatchToken(userid: UserId): String? {
    val userdir = userProfileDir(userid)
    userdir?: return null
    val syncTokenFile = File(userdir).resolve("next_batch")
    try {
        val batch = syncTokenFile.readText()
        syncTokenFile.delete()
        return batch
    } catch (e: IOException) {
        return null
    }
}
