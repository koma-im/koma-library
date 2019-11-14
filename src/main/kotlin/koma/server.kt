package koma

import koma.matrix.*
import koma.matrix.MatrixMediaApiDef
import koma.matrix.json.MoshiInstance
import koma.matrix.pagination.RoomBatch
import koma.matrix.room.naming.ResolveRoomAliasResult
import koma.matrix.user.AvatarUrl
import koma.matrix.user.identity.DisplayName
import koma.network.media.MHUrl
import koma.util.KResult
import koma.util.coroutine.adapter.retrofit.await
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.coroutine.adapter.retrofit.extractMatrix
import koma.util.flatMap
import koma.util.given
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * a matrix instance
 */
class Server(
        val url: HttpUrl,
        val httpClient: OkHttpClient,
        apiPath: String = "_matrix/client/r0/",
        mediaPath: String = "_matrix/media/r0/"
) {
    val apiURL = url.newBuilder().addPathSegments(apiPath).build()
    val mediaUrl = url.newBuilder().addPathSegments(mediaPath).build()

    private val downloader = Downloader(httpClient)

    private val retrofit = Retrofit.Builder()
            .baseUrl(apiURL)
            .addConverterFactory(MoshiConverterFactory.create(MoshiInstance.moshi))
            .client(httpClient).build()

    val service: MatrixPublicApi = retrofit.create(MatrixPublicApi::class.java)

    /**
     * needs authentication
     */
    private val userService = retrofit.create(MatrixAccessApiDef::class.java)

    private val mediaService = retrofit.newBuilder().baseUrl(mediaUrl).build()
            .create(MatrixMediaApiDef::class.java)
    init {
    }

    /**
     * get access to APIs that require auth
     */
    fun account(userId: UserId, token: String): MatrixApi {
        return MatrixApi(token, userId, this, userService, mediaService)
    }

    /**
     * name of a user
     */
    suspend fun getDisplayName(user: String): KResult<DisplayName, Failure> {
        return service.getDisplayName(user).await().flatMap { it.extractMatrix() }
    }

    suspend fun resolveRoomAlias(roomAlias: String): KResultF<ResolveRoomAliasResult> {
        val call: Call<ResolveRoomAliasResult> = service.resolveRoomAlias(roomAlias)
        return call.awaitMatrix()
    }

    suspend fun listPublicRooms(since: String?=null): KResultF<RoomBatch<DiscoveredRoom>> {
        return service.publicRooms(since).awaitMatrix()
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


/**
 * no access_token needed
 */
interface MatrixPublicApi {
    @GET("profile/{userId}/avatar_url")
    fun getAvatar(@Path("userId") user_id: UserId): Call<AvatarUrl>

    @GET("profile/{userId}/displayname")
    fun getDisplayName(@Path("userId") user_id: String
    ): Call<DisplayName>

    /**
     * just list, no filter
     */
    @GET("publicRooms")
    fun publicRooms(@Query("since") since: String? = null,
                    @Query("limit") limit: Int = 20
    ): Call<RoomBatch<DiscoveredRoom>>

    @GET("directory/room/{roomAlias}")
    fun resolveRoomAlias(@Path("roomAlias") roomAlias: String): Call<ResolveRoomAliasResult>

}

enum class ThumbnailMethod {
    Crop,
    Scale;

    fun toJson(): String = when(this) {
        Crop -> "crop"
        Scale -> "scale"
    }
}