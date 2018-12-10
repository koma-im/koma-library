package koma.gui.dialog.file.save

import okhttp3.HttpUrl

fun HttpUrl.guessFileName(): String {
    val ps = this.encodedPathSegments()
    val ls = ps.getOrNull(ps.lastIndex)
    return ls ?: ""
}
