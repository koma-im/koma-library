package koma.storage.config

import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

open class ConfigPaths(val config_home: String) {
    init {
        logger.debug { "using $config_home as data directory" }
        val dir = File(config_home)
        if (!dir.exists() && !dir.mkdir()) {
            logger.debug { "failed to create data directory $config_home" }
        }
    }

    val profile_dir = getOrCreate("profile", create = true)

    fun getCreateProfileDir(vararg paths: String, create: Boolean = true): String? {
        return getOrCreate(*paths, base = profile_dir, create = create)
    }

    fun getOrCreate(vararg paths: String, base: String? = null, create: Boolean = true): String? {
        var curdir = base ?: config_home
        for (p in paths) {
            curdir += File.separator + p
            val dir = File(curdir)
            if (!dir.exists()) {
                if (create) {
                    val result = dir.mkdir()
                    if (!result) {
                        println("failed to create $dir")
                        return null
                    }
                } else
                    return null
            }
        }
        return curdir
    }

    fun getCreateDir(vararg paths: String): File? {
        val ss = listOf(config_home, *paths)
        val path = ss.joinToString(File.separator)
        val file =File(path)
        return if (file.exists()) {
            if (file.isDirectory) file
            else null
        } else if(file.mkdirs()) {
            file
        } else null
    }
}

private fun getDefaultConfigDir(): String {
    val env = System.getenv()
    val config_home: String = env.get("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + File.separator + ".config")
    val config_dir = config_home + File.separator + "koma"
    val dir: File = File(config_dir)
    if (!dir.isDirectory()) {
        dir.mkdir()
    }
    return config_dir
}
