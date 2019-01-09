package koma.matrix

import koma.controller.sync.longPollTimeout
import koma.matrix.event.EventId
import koma.matrix.event.context.ContextResponse
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.event.room_message.state.RoomNameContent
import koma.matrix.json.MoshiInstance
import koma.matrix.pagination.FetchDirection
import koma.matrix.pagination.RoomBatch
import koma.matrix.publicapi.rooms.RoomDirectoryQuery
import koma.matrix.room.admin.BanRoomResult
import koma.matrix.room.admin.CreateRoomResult
import koma.matrix.room.admin.CreateRoomSettings
import koma.matrix.room.admin.MemberBanishment
import koma.matrix.room.naming.ResolveRoomAliasResult
import koma.matrix.room.naming.RoomId
import koma.matrix.room.participation.LeaveRoomResult
import koma.matrix.room.participation.invite.InviteMemResult
import koma.matrix.room.participation.invite.InviteUserData
import koma.matrix.room.participation.join.JoinRoomResult
import koma.matrix.sync.SyncResponse
import koma.network.client.okhttp.AppHttpClient
import koma.storage.config.server.ServerConf
import koma.storage.config.server.getAddress
import koma.matrix.event.room_message.RoomEventType
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class SendResult(
        val event_id: EventId
)

class UpdateAvatarResult()

/**
 * Api that requires access_token
 * the api only needs to be defined as an interface
 * retrofit/moshi handles the rest
 */
internal interface MatrixAccessApiDef {
    @POST("createRoom")
    fun createRoom(@Query("access_token") token: String,
                   @Body roomSettings: CreateRoomSettings): Call<CreateRoomResult>

    @POST("rooms/{roomIdentifier}/join")
    fun joinRoom(@Path("roomIdentifier") roomIdentifier: String,
                 @Query("access_token") token: String)
            : Call<JoinRoomResult>

    @POST("rooms/{roomId}/leave")
    fun leaveRoom(@Path("roomId") roomId: RoomId,
                  @Query("access_token") token: String)
            : Call<LeaveRoomResult>

    @GET("directory/room/{roomAlias}")
    fun resolveRoomAlias(@Path("roomAlias") roomAlias: String): Call<ResolveRoomAliasResult>

    @PUT("directory/room/{roomAlias}")
    fun putRoomAlias(@Path("roomAlias") roomAlias: String,
                     @Query("access_token") token: String,
                     @Body roomInfo: RoomInfo): Call<EmptyResult>

    @DELETE("directory/room/{roomAlias}")
    fun deleteRoomAlias(@Path("roomAlias") roomAlias: String,
                        @Query("access_token") token: String
    ): Call<EmptyResult>

    @GET("publicRooms")
    fun publicRooms(@Query("since") since: String? = null,
                    @Query("limit") limit: Int = 20
    ): Call<RoomBatch<DiscoveredRoom>>

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
    ): Call<Chunked<RoomEvent>>

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
            @Path("txnId") txnId: Long,
            @Query("access_token") token: String,
            @Body message: M_Message): Call<SendResult>

    @PUT("rooms/{roomId}/state/{eventType}")
    fun sendStateEvent(
            @Path("roomId") roomId: RoomId,
            @Path("eventType") type: RoomEventType,
            @Query("access_token") token: String,
            @Body content: Any): Call<SendResult>

    @GET("rooms/{roomId}/context/{eventId}")
    fun getEventContext(@Path("roomId") roomId: RoomId,
                 @Path("eventId") eventId: EventId,
                        @Query("limit") limit: Int = 2,
                 @Query("access_token") token: String
    ): Call<ContextResponse>


    @GET("sync")
    fun getEvents(@Query("since") from: String? = null,
                  @Query("access_token") token: String,
                  @Query("full_state") full_state: Boolean = false,
                  @Query("timeout") timeout: Int = longPollTimeout * 1000,
                  @Query("filter") filter: String? = null)
            : Call<SyncResponse>

   @PUT("profile/{userId}/avatar_url")
    fun updateAvatar(@Path("userId") user_id: UserId,
                     @Query("access_token") token: String,
                     @Body avatarUrl: AvatarUrl): Call<UpdateAvatarResult>

    @PUT("profile/{userId}/displayname")
    fun updateDisplayName(@Path("userId") user_id: UserId,
                     @Query("access_token") token: String,
                     @Body body: Map<String, String>): Call<EmptyResult>

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

class MatrixApi(
        private val token: String,
        val userId: UserId,
        /***
         * homeserver base address such as https://matrix.org
         */
        server: HttpUrl,
        apiPath: String = "_matrix/client/r0/",
        mediaPath: String = "_matrix/media/r0/",
        /**
         * share OkHttpClient as much as possible to conserve resources
         */
        http: AppHttpClient) {

    internal val service: MatrixAccessApiDef
    private val longPollService: MatrixAccessApiDef
    private val mediaService: MatrixMediaApiDef

    private val txnIdUnique = AtomicLong()

    fun getTxnId() = txnIdUnique.getAndAdd(1)

    fun createRoom(settings: CreateRoomSettings): Call<CreateRoomResult> {
        return service.createRoom(token, settings)
    }

    fun getRoomMessages(roomId: RoomId, from: String, direction: FetchDirection, to: String?=null): Call<Chunked<RoomEvent>> {
        return service.getMessages(roomId, token, from, direction, to=to)
    }

    fun joinRoom(roomid: RoomId): Call<JoinRoomResult> {
        return service.joinRoom(roomid.id, token)
    }

    fun getEventContext(roomid: RoomId, eventId: EventId): Call<ContextResponse> {
        return service.getEventContext(roomid, eventId,token= token)
    }

    fun uploadFile(file: File, contentType: MediaType): Call<UploadResponse> {
        val req = RequestBody.create(contentType, file)
        return mediaService.uploadMedia(contentType.toString(), token, req)
    }

    fun inviteMember(
          room: RoomId,
          memId: UserId): Call<InviteMemResult> =
            service.inviteUser(room.id, token, InviteUserData(memId))

    fun updateAvatar(user_id: UserId, avatarUrl: AvatarUrl): Call<UpdateAvatarResult>
            = service.updateAvatar(user_id, token, avatarUrl)

    fun updateDisplayName(newname: String): Call<EmptyResult>
            = service.updateDisplayName(
            this.userId, token,
            mapOf(Pair("displayname", newname)))

    fun setRoomIcon(roomId: RoomId, content: RoomAvatarContent):Call<SendResult>
            = service.sendStateEvent(roomId, RoomEventType.Avatar, token, content)

    fun banMember(
            roomid: RoomId,
            memId: UserId
    ): Call<BanRoomResult> = service.banUser(roomid.id, token, MemberBanishment(memId))

    fun leavingRoom(roomid: RoomId): Call<LeaveRoomResult>
            = service.leaveRoom(roomid, token)

    fun putRoomAlias(roomid: RoomId, alias: String): Call<EmptyResult>
            = service.putRoomAlias(alias, token, RoomInfo(roomid))

    fun deleteRoomAlias(alias: String): Call<EmptyResult>
            = service.deleteRoomAlias(alias, token)

    fun setRoomCanonicalAlias(roomid: RoomId, canonicalAlias: RoomCanonAliasContent)
            = service.sendStateEvent(roomid, RoomEventType.CanonAlias, token, canonicalAlias)

    fun setRoomName(roomid: RoomId, name: RoomNameContent)
            = service.sendStateEvent(roomid, RoomEventType.Name, token, name)

    fun resolveRoomAlias(roomAlias: String): Call<ResolveRoomAliasResult> {
        val call: Call<ResolveRoomAliasResult> = service.resolveRoomAlias(roomAlias)
        return call
    }

    fun sendRoomMessage(roomId: RoomId, message: M_Message): Call<SendResult> {
        println("sending message $message to room $roomId ")
        return service.sendMessageEvent(roomId, RoomEventType.Message, getTxnId(), token, message)
    }

    fun findPublicRooms(query: RoomDirectoryQuery) = service.findPublicRooms(token, query)

    fun getEvents(from: String?): Call<SyncResponse>
            = longPollService.getEvents(from, token)

    init {
        val moshi = MoshiInstance.moshi
        val apiURL = server.newBuilder().addPathSegments(apiPath).build()
        val rb = Retrofit.Builder()
                .baseUrl(apiURL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))

        service = rb.client(http.client).build()
                .create(MatrixAccessApiDef::class.java)

        // need to set a longer timeout for the sync api
        val longPollClient = http.builder.readTimeout(longPollTimeout.toLong() + 10, TimeUnit.SECONDS).build()
        longPollService = rb.client(longPollClient).build().create(MatrixAccessApiDef::class.java)

        val mu = server.newBuilder().addPathSegments(mediaPath).build()
        mediaService = Retrofit.Builder()
                .baseUrl(mu)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(http.client)
                .build().create(MatrixMediaApiDef::class.java)
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

fun login(userpass: UserPassword, serverConf: ServerConf, http: AppHttpClient):
        Call<AuthedUser> {
    val moshi = MoshiInstance.moshi
    val client = http.client
    val retrofit = Retrofit.Builder()
            .baseUrl(serverConf.getAddress())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    val service = retrofit.create(MatrixLoginApi::class.java)
    val auth_call: Call<AuthedUser> = service.login(userpass)
    return auth_call
}
