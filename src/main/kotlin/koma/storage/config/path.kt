package koma.storage.config

import java.io.File

fun getHttpCacheDir(dir: String): File? {
    val p = File(dir).resolve("koma").resolve("cache").resolve("http")
    return if (p.exists()) {
        if (p.isDirectory) p else null
    } else if(p.mkdirs()) {
        p
    } else null
}
