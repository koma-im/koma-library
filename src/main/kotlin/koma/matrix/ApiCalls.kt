package koma.matrix

import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.request.*
import io.ktor.content.ByteArrayContent
import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.http.content.LocalFileContent
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.*
import kotlin.time.Duration
import kotlin.time.seconds

private val logger = KotlinLogging.logger {}

data class SendResult(
        val event_id: EventId
)

@Serializable
class UpdateAvatarResult()

/**
 * Api that requires access_token
 * the api only needs to be defined as an interface
 * retrofit/moshi handles the rest
 */
interface MatrixAccessApiDef {
    @POST("rooms/{roomId}/join")
    fun joinRoom(@Path("roomId") roomId: String,
                 @Query("access_token") token: String)
            : Call<JoinRoomResult>

    @POST("rooms/{roomId}/leave")
    fun leaveRoom(@Path("roomId") roomId: String,
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
            @Path("roomId") roomId: String,
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
    fun getEventContext(@Path("roomId") roomId: String,
                 @Path("eventId") eventId: String,
                        @Query("limit") limit: Int = 2,
                 @Query("access_token") token: String
    ): Call<ContextResponse>

    @PUT("profile/{userId}/displayname")
    fun updateDisplayName(@Path("userId") user_id: String,
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
        val server: Server
) {
    private val txnId = AtomicLong()
    private fun getTxnId(): String {
        val t = System.currentTimeMillis()
        val id = txnId.accumulateAndGet(t) { value, given ->
            if (given > value) given else value + 1
        }
        return id.toString()
    }

    private fun HttpRequestBuilder.buildUrl(vararg pathSegments: String) {
        url {
            takeFrom(server.apiUrlKtor)
            path(*server.apiUrlPath, *pathSegments)
        }
        parameter("access_token", token)
    }
    private fun HttpRequestBuilder.jsonBody(body: Any) {
        contentType(ContentType.Application.Json)
        this.body = body
    }

    internal suspend inline fun <reified T> request(
            method: HttpMethod,
            crossinline block: HttpRequestBuilder.() -> Unit
    ): KResult<T, KomaFailure>  {
        return server.ktorHttpClient.requestResult<T>(method, block)
    }

    suspend fun createRoom(settings: CreateRoomSettings): KResultF<CreateRoomResult> {
        return request(method = HttpMethod.Post) {
            contentType(ContentType.Application.Json)
            buildUrl("createRoom")
            body = settings
        }
    }

    suspend fun getNotifications(from: String? = null, limit: Int? = null, only: String? = null
    ): KResultF<NotificationResponse> {
        return request(method = HttpMethod.Get) {
            buildUrl("notifications")
            parameter("from", from)
            parameter("limit", limit)
            parameter("only", only)
        }
    }

    suspend fun getRoomMessages(roomId: RoomId, from: String, direction: FetchDirection, to: String?=null
    ): KResultF<Chunked<@Serializable(with=RawSerializer::class) Preserved<RoomEvent>>> {
        return request(method = HttpMethod.Get) {
            buildUrl("rooms", roomId.full, "messages")
            parameter("from", from)
            parameter("to", to)
            parameter("dir", direction.toName())
        }
    }

    suspend fun joinRoom(roomid: RoomId): KResultF<JoinRoomResult> {
        return request(method = HttpMethod.Post) {
            buildUrl("rooms", roomid.full, "join")
        }
    }

    suspend fun getEventContext(roomid: RoomId, eventId: EventId, limit: Int = 2): KResultF<ContextResponse> {
        return request(method = HttpMethod.Get) {
            buildUrl("rooms", roomid.full, "context", eventId.full)
            parameter("limit", limit)
        }
    }

    suspend fun uploadMedia(body: Any, type: ContentType) : KResultF<UploadResponse> {
        return request(method = HttpMethod.Post) {
            contentType(type)
            buildUrl("upload")
            this.body = body
        }
    }
    suspend fun uploadFile(file: File, contentType: ContentType): KResultF<UploadResponse> {
        return uploadMedia(LocalFileContent(file), contentType)
    }
    suspend fun uploadByteArray(contentType: ContentType, byteArray: ByteArray): KResultF<UploadResponse> {
        return uploadMedia(ByteArrayContent(byteArray), contentType)
    }

    suspend fun inviteMember(
          room: RoomId,
          memId: UserId): KResultF<InviteMemResult> {
        return request(method = HttpMethod.Post) {
            buildUrl("rooms", room.full, "invite")
            jsonBody(InviteUserData(memId))
        }
    }


    suspend fun updateAvatar(user_id: UserId, avatarUrl: AvatarUrl
    ): KResultF<UpdateAvatarResult> {
        val u = user_id.full
        return runCatch {
            server.ktorHttpClient.put<UpdateAvatarResult> {
                contentType(ContentType.Application.Json)
                buildUrl("profile", u, "avatar_url")
                url {
                    parameter("access_token", token)
                }
                body = avatarUrl
            }
        }.mapFailure {
            it.toFailure()
        }
    }

    suspend fun updateDisplayName(newname: String): KResultF<EmptyResult> {
        val u = this.userId.full
        return request(method = HttpMethod.Put) {
            buildUrl("profile", u, "displayname")
            jsonBody(DisplayName(newname))
        }
    }

    suspend fun setRoomIcon(roomId: RoomId, content: RoomAvatarContent):KResultF<SendResult> {
        return request(method = HttpMethod.Put) {
            buildUrl("rooms", roomId.full, "state", RoomEventType.Avatar.toName())
            jsonBody(content)
        }
    }

    suspend fun banMember(
            roomid: RoomId,
            memId: UserId
    ): KResultF<BanRoomResult> {
        return request(method = HttpMethod.Post) {
            buildUrl("rooms", roomid.full, "ban")
            jsonBody(MemberBanishment(memId))
        }
    }

    suspend fun leavingRoom(roomid: RoomId): KResultF<LeaveRoomResult> {
        return request(method = HttpMethod.Post) {
            buildUrl("rooms", roomid.full, "leave")
        }
    }


    suspend fun putRoomAlias(roomid: RoomId, alias: String): KResultF<EmptyResult> {
        return request(method = HttpMethod.Put) {
            buildUrl("directory", "room", alias)
            jsonBody(RoomInfo(roomid))
        }
    }


    suspend fun deleteRoomAlias(alias: String): KResultF<EmptyResult> {
        return request(method = HttpMethod.Delete) {
            buildUrl("directory", "room", alias)
        }
    }

    internal suspend inline fun putStateEvent(roomId: RoomId, type: RoomEventType, body: Any): KResult<SendResult, KomaFailure> {
        return request(method = HttpMethod.Put) {
            buildUrl("rooms", roomId.full, "state", type.toName())
            jsonBody(body)
        }
    }

    internal suspend inline fun<reified T> getStateEvent(roomId: RoomId, type: RoomEventType): KResult<T, KomaFailure> {
        return request(method = HttpMethod.Get) {
            buildUrl("rooms", roomId.full, "state", type.toName())
        }
    }

    suspend fun setRoomCanonicalAlias(roomid: RoomId, canonicalAlias: RoomCanonAliasContent): KResult<SendResult, KomaFailure> {
        return putStateEvent(roomid, RoomEventType.CanonAlias, canonicalAlias)
    }


    suspend fun setRoomName(roomid: RoomId, name: RoomNameContent): KResult<SendResult, KomaFailure> {
        return putStateEvent(roomid, RoomEventType.Name, name)
    }

    suspend fun getRoomName(roomId: RoomId): Result<Optional<String>, Failure> {
        val r = getStateEvent<JsonObject>(roomId, RoomEventType.Name)
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
        val (r, e, x) = getStateEvent<JsonObject>(roomId, RoomEventType.Avatar)
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
    internal suspend inline fun<reified T> putMessageEvent(
            roomId: RoomId, type: RoomEventType, txnId: String, body: M_Message
    ): KResult<T, KomaFailure> {
        return request(method = HttpMethod.Put) {
            buildUrl("rooms", roomId.full, "send", type.toName(), txnId)
            jsonBody(body)
        }
    }
    suspend fun sendMessage(roomId: RoomId, message: M_Message
    ): Result<SendResult, Failure> {
        val tid = getTxnId()
        logger.info { "sending to room $roomId message $tid with content $message" }
        return putMessageEvent(roomId, RoomEventType.Message, tid, message)
    }

    suspend fun findPublicRooms(query: RoomDirectoryQuery): Result<RoomBatch<DiscoveredRoom>, Failure>{
        return request(method = HttpMethod.Post) {
            buildUrl("publicRooms")
            jsonBody(query)
        }
    }

    internal val longTimeoutClient = server.ktorHttpClient.config {
        engine {
            check(this is OkHttpConfig)
            this.config {
                readTimeout(100, TimeUnit.SECONDS)
            }
        }
    }
    private val longClient = server.httpClient.newBuilder().readTimeout(100, TimeUnit.SECONDS).build()
    private val syncUrl = server.apiURL.newBuilder().addPathSegment("sync").build()
    suspend fun sync(since: String?
                            , timeout: Duration = 50.seconds
                            , full_state: Boolean = false
                            , filter: String? = null
                            , networkTimeout: Duration = timeout + 10.seconds
    ): Result<SyncResponse, KomaFailure> {
        val (success,failure, result) =withTimeout(networkTimeout) {
            longTimeoutClient.requestResult<SyncResponse>(HttpMethod.Get) {
                buildUrl("sync")
                parameter("since", since)
                parameter("timeout", timeout.toLongMilliseconds())
                parameter("full_state", full_state)
                parameter("filter", filter)
            }
        }
        if (result.testFailure(success, failure)) {
            return KResult.failure(failure)
        }
        return success
    }
}

@Serializable
data class AuthedUser(
        val access_token: String,
        val user_id: UserId)

@Serializable
data class UserPassword(
        val type: String = "m.login.password",
        // name only, without @ or :
        val user: String,
        val password: String
)