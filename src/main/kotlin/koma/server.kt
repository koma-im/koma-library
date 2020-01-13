@file:Suppress("EXPERIMENTAL_API_USAGE")

package koma

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.http.*
import koma.matrix.*
import koma.matrix.json.jsonDefaultConf
import koma.matrix.json.jsonOmit
import koma.matrix.room.naming.ResolveRoomAliasResult
import koma.matrix.user.identity.DisplayName
import koma.network.media.MHUrl
import koma.util.KResult
import koma.util.given
import koma.util.requestResult
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * a matrix instance
 */
class Server(
        val url: HttpUrl,
        val okHttpClient: OkHttpClient,
        /**
         * must end with a slash, required by retrofit
         */
        apiPath: String = "_matrix/client/r0/",
        mediaPath: String = "_matrix/media/r0/"
) {
    val apiURL = url.newBuilder().addPathSegments(apiPath).build()
    val mediaUrl = url.newBuilder().addPathSegments(mediaPath).build()
    internal val apiUrlPath = apiURL.pathSegments().filterNot { it.isEmpty() }.toTypedArray()
    internal val apiUrlKtor = URLBuilder(apiURL.toString()).build()
    internal fun URLBuilder.buildPath(vararg pathSegments: String) {
        takeFrom(apiUrlKtor)
        path(*apiUrlPath, *pathSegments)
    }
    internal val mediaPathSegments = url.pathSegments().toTypedArray().plus(arrayOf("_matrix", "media", "r0"))

    @Deprecated("moving toward multi-platform", ReplaceWith("okHttpClient"))
    val httpClient
        get() = okHttpClient

    val ktorHttpClient: HttpClient

    internal val longTimeoutClient: HttpClient
    private val downloader = Downloader(okHttpClient)

    init {
        val contentTypes = listOf(ContentType.Application.Json, ContentType.Text.Html)
        val kserializer = KotlinxSerializer(jsonOmit)
        ktorHttpClient = HttpClient(OkHttp) {
            install(JsonFeature) {
                acceptContentTypes = contentTypes
                serializer = kserializer
            }
            engine {
                preconfigured = okHttpClient
            }
        }
        longTimeoutClient = HttpClient(OkHttp) {
            install(JsonFeature) {
                acceptContentTypes = contentTypes
                serializer = kserializer
            }
            engine {
                preconfigured = okHttpClient.newBuilder().readTimeout(100, TimeUnit.SECONDS).build()
            }
        }
    }

    /**
     * get access to APIs that require auth
     */
    fun account(userId: UserId, token: String): MatrixApi {
        return MatrixApi(token, userId, this)
    }

    private suspend inline fun <reified T: Any> request(
            method: HttpMethod,
            crossinline block: HttpRequestBuilder.() -> Unit
    ): KResult<T, KomaFailure>  {
        return ktorHttpClient.requestResult<T>(method, block)
    }

    /**
     * name of a user
     */
    suspend fun getDisplayNameKtor(userId: UserId): KResult<DisplayName, Failure> {
        return request(HttpMethod.Get) {
            url {
                buildPath("profile", userId.full, "displayname")
            }
        }
    }

    suspend fun resolveRoomAlias(roomAlias: String): KResultF<ResolveRoomAliasResult> {
        return request(HttpMethod.Get) {
            url {
                buildPath("directory", "room", roomAlias)
            }
        }
    }

    @Deprecated("may fail without authentication")
    suspend fun listPublicRooms(since: String?=null, limit: Int = 20
    ): KResultF<RoomListing> {
        return request(HttpMethod.Get) {
            url {
                buildPath("publicRooms")
                parameter("since", since)
                parameter("limit", limit)
            }
        }
    }

    suspend fun login(userpass: UserPassword): KResultF<AuthedUser> {
        return request<AuthedUser>(HttpMethod.Post) {
            contentType(ContentType.Application.Json)
            url {
                buildPath("login")
            }
            body = userpass
        }
    }

    suspend fun registerWithPassword(password: String,
                                     username: String? = null
    ): KResultF<RegistrationResponse> {
        val data = RegistrationData(
                password = password,
                username = username,
                auth = AuthenticationData(type = "m.login.dummy")
        )
        return request(HttpMethod.Post) {
            contentType(ContentType.Application.Json)
            url {
                buildPath("register")
            }
            body = data
        }
    }


    fun mxcToHttp(mxc: MHUrl): HttpUrl {
        when (mxc){
            is MHUrl.Http->return mxc.http
            is MHUrl.Mxc->{
                return mediaUrl.newBuilder()
                        .addPathSegment("download")
                        .addPathSegment(mxc.server)
                        .addPathSegment(mxc.media)
                        .build()
            }
        }
    }

    suspend fun downloadMedia(mhUrl: MHUrl): KResult<ByteArray, KomaFailure> {
        when (mhUrl) {
            is MHUrl.Mxc -> {
                val u = mxcToHttp(mhUrl)
                return downloader.downloadMedia(u, Integer.MAX_VALUE)
            }
            is MHUrl.Http -> return sharedDownloader().downloadMedia(mhUrl.http)
        }
    }

    suspend fun getThumbnail(mxc: MHUrl.Mxc
                             , width: UShort
                             , height: UShort
                             , method: ThumbnailMethod? = null
                             , allowRemote: Boolean? = null
    ): KResult<ByteArray, KomaFailure> {
        val (w, h) = when {
            (width <= 32u && height <= 32u) -> 32u to 32u
            width <= 96u && height <= 96u -> 96u to 96u
            width <= 320u && height <= 240u -> 320u to 240u
            width <=640u && height <=480u -> 640u to 480u
            else -> 800u to 600u
        }
        val m = method ?: if (w < 128u) ThumbnailMethod.Crop else ThumbnailMethod.Scale
        val u = mediaUrl.newBuilder()
                .addPathSegment("thumbnail")
                .addPathSegment(mxc.server)
                .addPathSegment(mxc.media)
                .addQueryParameter("width", w.toString())
                .addQueryParameter("height", h.toString())
                .addQueryParameter("method", m.toJson())
                .given(allowRemote) {addQueryParameter("allow_remote", it.toString())}
                .build()
        return downloader.downloadMedia(u, Integer.MAX_VALUE)
    }

    /**
     * for downloading resources that are not necessarily on this server
     */
    private fun sharedDownloader(): Downloader {
        val s = sharedDownloader
        if (s!= null) return s
        logger.info { "creating shared Downloader" }
        sharedDownloader = Downloader(httpClient)
        return sharedDownloader!!
    }
    companion object {
        @Volatile
        private var sharedDownloader: Downloader? = null
    }
}

enum class ThumbnailMethod {
    Crop,
    Scale;

    fun toJson(): String = when(this) {
        Crop -> "crop"
        Scale -> "scale"
    }
}