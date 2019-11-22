package koma.matrix

import koma.*
import koma.KResultF
import koma.util.KResult as Result
import koma.matrix.event.EventId
import koma.matrix.event.context.ContextResponse
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.event.room_message.state.RoomNameContent
import koma.matrix.json.*
import koma.matrix.pagination.FetchDirection
import koma.matrix.pagination.RoomBatch
import koma.matrix.publicapi.rooms.RoomDirectoryQuery
import koma.matrix.room.admin.BanRoomResult
import koma.matrix.room.admin.CreateRoomResult
import koma.matrix.room.admin.CreateRoomSettings
import koma.matrix.room.admin.MemberBanishment
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.LeaveRoomResult
import koma.matrix.room.participation.invite.InviteMemResult
import koma.matrix.room.participation.invite.InviteUserData
import koma.matrix.room.participation.join.JoinRoomResult
import koma.matrix.sync.SyncResponse
import koma.matrix.user.AvatarUrl
import koma.matrix.user.identity.DisplayName
import koma.util.*
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import koma.util.KResult
import koma.util.coroutine.adapter.okhttp.awaitType
import koma.util.coroutine.withTimeout
import kotlinx.coroutines.internal.artificialFrame
import okhttp3.*
import kotlin.time.Duration
import kotlin.time.seconds

private val logger = KotlinLogging.logger {}

data class SendResult(
        val event_id: EventId
)

class UpdateAvatarResult()

/**
 * Api that requires access_token
 * the api only needs to be defined as an interface
 * retrofit/moshi handles the rest
 */
interface MatrixAccessApiDef {
    @POST("createRoom")
    fun createRoom(@Query("access_token") token: String,
                   @Body roomSettings: CreateRoomSettings): Call<CreateRoomResult>

    @POST("rooms/{roomId}/join")
    fun joinRoom(@Path("roomId") roomId: String,
                 @Query("access_token") token: String)
            : Call<JoinRoomResult>

    @POST("rooms/{roomId}/leave")
    fun leaveRoom(@Path("roomId") roomId: RoomId,
                  @Query("access_token") token: String)
            : Call<LeaveRoomResult>

    @PUT("directory/room/{roomAlias}")
    fun putRoomAlias(@Path("roomAlias") roomAlias: String,
                     @Query("access_token") token: String,
                     @Body roomInfo: RoomInfo): Call<EmptyResult>

    @DELETE("directory/room/{roomAlias}")
    fun deleteRoomAlias(@Path("roomAlias") roomAlias: String,
                        @Query("access_token") token: String
    ): Call<EmptyResult>

    @POST("publicRooms")
    fun findPublicRooms(
            @Query("access_token") token: String,
            @Body query: RoomDirectoryQuery
    ): Call<RoomBatch<DiscoveredRoom>>

    @GET("rooms/{roomId}/messages")
    fun getMessages(
            @Path("roomId") roomId: RoomId,
            @Query("access_token") token: String,
            @Query("from") from: String,
            @Query("dir") dir: FetchDirection,
            // optional params
            @Query("limit") limit: Int = 100,
            @Query("to") to: String? = null
    ): Call<Chunked<RawJson<RoomEvent>>>

    @POST("rooms/{roomId}/invite")
    fun inviteUser(@Path("roomId") roomId: String,
                   @Query("access_token") token: String,
                   @Body invitation: InviteUserData
    ): Call<InviteMemResult>

    @POST("rooms/{roomId}/ban")
    fun banUser(@Path("roomId") roomId: String,
                @Query("access_token") token: String,
                @Body banishment: MemberBanishment
    ): Call<BanRoomResult>

    @PUT("rooms/{roomId}/send/{eventType}/{txnId}")
    fun sendMessageEvent(
            @Path("roomId") roomId: RoomId,
            @Path("eventType") eventType: RoomEventType,
            @Path("txnId") txnId: String,
            @Query("access_token") token: String,
            @Body message: M_Message): Call<SendResult>

    @PUT("rooms/{roomId}/state/{eventType}")
    fun sendStateEvent(
            @Path("roomId") roomId: RoomId,
            @Path("eventType") type: RoomEventType,
            @Query("access_token") token: String,
            @Body content: Any): Call<SendResult>

    @GET("rooms/{roomId}/state/{eventType}")
    fun getStateEvent(
            @Path("roomId") roomId: RoomId,
            @Path("eventType") type: RoomEventType,
            @Query("access_token") token: String
    ): Call<Map<String, Any>>

    @GET("rooms/{roomId}/context/{eventId}")
    fun getEventContext(@Path("roomId") roomId: RoomId,
                 @Path("eventId") eventId: EventId,
                        @Query("limit") limit: Int = 2,
                 @Query("access_token") token: String
    ): Call<ContextResponse>

    @GET("notifications")
    fun getNotifications(
            @Query("access_token") token: String,
            @Query("from") from: String? = null,
            @Query("limit") limit: Int? = null,
            @Query("only") only: String? = null
    ): Call<NotificationResponse>

    @PUT("profile/{userId}/avatar_url")
    fun updateAvatar(@Path("userId") user_id: String,
                     @Query("access_token") token: String,
                     @Body avatarUrl: AvatarUrl): Call<UpdateAvatarResult>


    @PUT("profile/{userId}/displayname")
    fun updateDisplayName(@Path("userId") user_id: UserId,
                     @Query("access_token") token: String,
                     @Body body: DisplayName): Call<EmptyResult>
}

/**
 * usually at path _matrix/media/r0/
 */
internal interface MatrixMediaApiDef {
    @POST("upload")
    fun uploadMedia(@Header("Content-Type") type: String,
                    @Query("access_token") token: String,
                    @Body content: RequestBody
    ): Call<UploadResponse>
}

class MatrixApi internal constructor(
        private val token: String,
        val userId: UserId,
        val server: Server,
        private val service: MatrixAccessApiDef,
        private val mediaService: MatrixMediaApiDef
) {
    private val txnId = AtomicLong()
    private fun getTxnId(): String {
        val t = System.currentTimeMillis()
        val id = txnId.accumulateAndGet(t) { value, given ->
            if (given > value) given else value + 1
        }
        return id.toString()
    }

    suspend fun createRoom(settings: CreateRoomSettings): KResultF<CreateRoomResult> {
        return service.createRoom(token, settings).awaitMatrix()
    }

    suspend fun getNotifications(from: String? = null, limit: Int? = null, only: String? = null
    ): KResultF<NotificationResponse> {
        return service.getNotifications(token, from, limit, only).awaitMatrix()
    }

    suspend fun getRoomMessages(roomId: RoomId, from: String, direction: FetchDirection, to: String?=null
    ): KResultF<Chunked<RawJson<RoomEvent>>> {
        return service.getMessages(roomId, token, from, direction, to=to).awaitMatrix()
    }

    suspend fun joinRoom(roomid: RoomId): KResultF<JoinRoomResult> {
        return service.joinRoom(roomid.id, token).awaitMatrix()
    }

    suspend fun getEventContext(roomid: RoomId, eventId: EventId): KResultF<ContextResponse> {
        return service.getEventContext(roomid, eventId,token= token).awaitMatrix()
    }

    suspend fun uploadFile(file: File, contentType: MediaType): KResultF<UploadResponse> {
        val req = RequestBody.create(contentType, file)
        return mediaService.uploadMedia(contentType.toString(), token, req).awaitMatrix()
    }
    suspend fun uploadByteArray(contentType: MediaType, byteArray: ByteArray): KResultF<UploadResponse> {
        val req = RequestBody.create(contentType, byteArray)
        return mediaService.uploadMedia(contentType.toString(), token, req).awaitMatrix()
    }

    suspend fun inviteMember(
          room: RoomId,
          memId: UserId): KResultF<InviteMemResult> =
            service.inviteUser(room.id, token, InviteUserData(memId)).awaitMatrix()

    suspend fun updateAvatar(user_id: UserId, avatarUrl: AvatarUrl
    ): KResultF<UpdateAvatarResult> {
        val u = user_id.full
        return service.updateAvatar(u, token, avatarUrl).awaitMatrix()
    }

    suspend fun updateDisplayName(newname: String): KResultF<EmptyResult>
            = service.updateDisplayName(
            this.userId, token,
            DisplayName(newname)).awaitMatrix()

    suspend fun setRoomIcon(roomId: RoomId, content: RoomAvatarContent):KResultF<SendResult>
            = service.sendStateEvent(roomId, RoomEventType.Avatar, token, content).awaitMatrix()

    suspend fun banMember(
            roomid: RoomId,
            memId: UserId
    ): KResultF<BanRoomResult> = service.banUser(roomid.id, token, MemberBanishment(memId)).awaitMatrix()

    suspend fun leavingRoom(roomid: RoomId): KResultF<LeaveRoomResult>
            = service.leaveRoom(roomid, token).awaitMatrix()

    suspend fun putRoomAlias(roomid: RoomId, alias: String): KResultF<EmptyResult>
            = service.putRoomAlias(alias, token, RoomInfo(roomid)).awaitMatrix()

    suspend fun deleteRoomAlias(alias: String): KResultF<EmptyResult>
            = service.deleteRoomAlias(alias, token).awaitMatrix()

    suspend fun setRoomCanonicalAlias(roomid: RoomId, canonicalAlias: RoomCanonAliasContent)
            = service.sendStateEvent(roomid, RoomEventType.CanonAlias, token, canonicalAlias).awaitMatrix()

    suspend fun setRoomName(roomid: RoomId, name: RoomNameContent)
            = service.sendStateEvent(roomid, RoomEventType.Name, token, name).awaitMatrix()

    suspend fun getRoomName(roomId: RoomId): Result<Optional<String>, Failure> {
        val r = service.getStateEvent(roomId, RoomEventType.Name, token).awaitMatrix()
        val s = r.getOrNull() ?: return run{
            val e = r.failureOrThrow()
            if (e is HttpFailure && e.http_code == 404) {
                Result.success(Optional.empty())
            }else if (e is MatrixFailure && e.errcode == "M_NOT_FOUND") {
                Result.success(Optional.empty())
            } else {
                Result.failure(e)
            }
        }
        val n = s["name"]?.toString()
        return Result.success(Optional.ofNullable(n))
    }
    suspend fun getRoomAvatar(roomId: RoomId): Result<Optional<String>, Failure> {
        val (r, e, x) = service.getStateEvent(roomId, RoomEventType.Avatar, token).awaitMatrix()
        if (x.testFailure(r, e)) {
            return if (e is HttpFailure && e.http_code == 404) {
                Result.success(Optional.empty())
            } else if (e is MatrixFailure && e.errcode == "M_NOT_FOUND") {
                Result.success(Optional.empty())
            }   else{
                Result.failure(e)
            }
        } else {
            logger.debug { "got room avatar ${r}" }
            return Result.success(Optional.ofNullable(r["url"]?.toString()))
        }
    }

    suspend fun sendMessage(roomId: RoomId, message: M_Message
    ): Result<SendResult, Failure> {
        val tid = getTxnId()
        logger.info { "sending to room $roomId message $tid with content $message" }

        val res = service.sendMessageEvent(roomId, RoomEventType.Message, tid, token, message).awaitMatrix()

        return res
    }

    suspend fun findPublicRooms(query: RoomDirectoryQuery) = service.findPublicRooms(token, query).awaitMatrix()

    private val longClient = server.httpClient.newBuilder().readTimeout(100, TimeUnit.SECONDS).build()
    private val syncUrl = server.apiURL.newBuilder().addPathSegment("sync").build()
    suspend fun sync(since: String?
                            , timeout: Duration = 50.seconds
                            , full_state: Boolean = false
                            , filter: String? = null
                            , networkTimeout: Duration = timeout + 10.seconds
    ): Result<SyncResponse, KomaFailure> {
        val url = syncUrl.newBuilder().addQueryParameter("access_token", token)
                .given(since) {addQueryParameter("since", it)}
                .addQueryParameter("timeout", timeout.toLongMilliseconds().toString())
                .addQueryParameter("full_state", full_state.toString())
                .given(filter) {addQueryParameter("filter", it)}
                .build()
        val request = Request.Builder().url(url).build()
        val client = if (longClient.readTimeoutMillis() > networkTimeout.inMilliseconds) {
            longClient
        } else {
            longClient.newBuilder().readTimeout(networkTimeout.toLongMilliseconds()+1000, TimeUnit.MILLISECONDS).build()
        }
        val call = client.newCall(request)
        val (success,failure, result) = withTimeout(networkTimeout) {
            call.awaitType<SyncResponse>()
        }
        if (result.testFailure(success, failure)) {
            return KResult.failure(failure)
        }
        return success
    }
}

data class AuthedUser(
        val access_token: String,
        val user_id: UserId)

data class UserPassword(
        val type: String = "m.login.password",
        // name only, without @ or :
        val user: String,
        val password: String
)
interface MatrixLoginApi {
    @POST("_matrix/client/r0/login")
    fun login(@Body userpass: UserPassword): Call<AuthedUser>
}

suspend fun login(userpass: UserPassword, server: String, client: OkHttpClient):
        KResultF<AuthedUser> {
    val moshi = MoshiInstance.moshi
    val serverUrl = if (server.endsWith('/')) server else "$server/"
    val auth_call = runCatch {
        val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        val service = retrofit.create(MatrixLoginApi::class.java)
        service.login(userpass)
    }
    val c = if (auth_call.isFailure) {
        return KResult.failure(IOFailure(auth_call.failureOrThrow()))
    } else auth_call.getOrThrow()
    return c.awaitMatrix()
}
