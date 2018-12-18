package koma.storage.config.server

import com.squareup.moshi.Moshi
import koma.matrix.json.MoshiInstance
import koma.storage.config.ConfigPaths
import koma.storage.config.server.cert_trust.CompositeX509TrustManager
import koma.storage.config.server.cert_trust.loadContext
import okhttp3.HttpUrl
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext

class ServerConf(
        val servername: String,
        var addresses: MutableList<String>,
        val apiPath: String = "_matrix/client/r0/",
        val mediaPath: String? = "_matrix/media/r0/download"
)



const val conf_file_name = "server_conf.json"

/**
 * get preferred web address
 */
fun ServerConf.getAddress(): String {
    val addr = this.addresses.get(0)
    val slash = if (addr.endsWith('/')) addr else { addr.trimEnd('/') + "/" }
    return slash
}

fun ServerConf.getApiUrlBuilder(): HttpUrl.Builder? {
    val a = this.addresses.getOrNull(0) ?: return null
    val u = HttpUrl.parse(a) ?: return null
    return u.newBuilder().addPathSegments(apiPath)
}

/**
 * in practice null can happen
 */
fun ServerConf.getMediaPath()
        = mediaPath?:"_matrix/media/r0/download"

class ServerConfStore(private val paths: ConfigPaths) {
    private val configurations = ConcurrentHashMap<String, ServerConf>()

    /**
     * path used to store server settings
     */
    fun serverSettingsPath(servername: String): File? {
        return paths.getCreateDir("settings", "homeserver", servername)
    }

    fun saveServerAddress(serverConf: ServerConf, addr: String) {
        serverConf.addresses.remove(addr)
        serverConf.addresses.add(0, addr)
        val dir = serverSettingsPath(serverConf.servername)
        dir ?: return
        serverConf.save(dir)
    }

    fun loadServerCert(serverConf: ServerConf): Pair<SSLContext, CompositeX509TrustManager>? {
        val dir = serverSettingsPath(serverConf.servername)
        return dir?.let { loadContext(it) }
    }

    fun serverConf(servername: String): ServerConf {
        val conf = configurations.computeIfAbsent(servername, { computeServerConf(it) })
        return conf
    }

    fun serverConfWithAddr(servername: String, addr: String): ServerConf{
        val conf = serverConf(servername)
        saveServerAddress(conf, addr)
        return conf
    }

    fun serverConfFromAddr(addr: String): ServerConf? {
        val url = HttpUrl.parse(addr)?:return null
        val name = url.host()
        return serverConfWithAddr(name, addr)
    }

    private fun computeServerConf(servername: String): ServerConf {
        val serverConf = ServerConf(
                servername,
                mutableListOf()
        )
        val dir = serverSettingsPath(servername)
        dir?: return serverConf
        val sf = dir.resolve(conf_file_name)
        val jsonAdapter = MoshiInstance.moshi
                .adapter(ServerConf::class.java)
        val loadedConf = try {
            jsonAdapter.fromJson(sf.readText())
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        return  loadedConf?: serverConf
    }
}

fun ServerConf.save(dir: File) {
    val moshi = Moshi.Builder()
            .build()
    val jsonAdapter = moshi.adapter(ServerConf::class.java).indent("    ")
    val json = try {
        jsonAdapter.toJson(this)
    } catch (e: ClassCastException) {
        e.printStackTrace()
        return
    }
    try {
        val file = dir.resolve(conf_file_name)
        file.writeText(json)
    } catch (e: IOException) {
    }
}



