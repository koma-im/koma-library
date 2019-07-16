package koma

import koma.matrix.DiscoveredRoom
import koma.matrix.MatrixApi
import koma.matrix.UserId
import koma.matrix.json.MoshiInstance
import koma.matrix.media.parseMediaUrl
import koma.matrix.pagination.RoomBatch
import koma.matrix.room.naming.ResolveRoomAliasResult
import koma.matrix.user.AvatarUrl
import koma.matrix.user.identity.DisplayName
import koma.util.KResult
import koma.util.KResult as Result
import koma.util.coroutine.adapter.retrofit.await
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.coroutine.adapter.retrofit.extractMatrix
import koma.util.flatMap
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * a matrix instance
 */
class Server(
        val url: HttpUrl,
        val km: Koma,
        apiPath: String = "_matrix/client/r0/",
        private val mediaPath: String = "_matrix/media/r0/"
) {
    val apiURL = url.newBuilder().addPathSegments(apiPath).build()
    val mediaUrl = url.newBuilder().addPathSegments(mediaPath).build()
    val service: MatrixPublicApi
    init {
        val moshi = MoshiInstance.moshi
        val rb = Retrofit.Builder()
                .baseUrl(apiURL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))

        service = rb.client(km.http.client).build()
                .create(MatrixPublicApi::class.java)
    }

    /**
     * get access to APIs that require auth
     */
    fun account(userId: UserId, token: String): MatrixApi {
        return MatrixApi(token, userId, this)
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

    fun getMediaUrl(addr: String): Result<HttpUrl, KomaFailure> {
        return parseMediaUrl(addr, url, mediaPath)
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