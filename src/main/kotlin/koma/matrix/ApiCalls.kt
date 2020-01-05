package koma.matrix

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.LocalFileContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import koma.*
import koma.matrix.event.EventId
import koma.matrix.event.context.ContextResponse
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.RoomEventSerializer
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.event.room_message.chat.M_Message
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.event.room_message.state.RoomNameContent
import koma.matrix.json.Preserved
import koma.matrix.json.RawSerializer
import koma.matrix.pagination.FetchDirection
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
import koma.matrix.user.presence.UserPresenceType
import koma.util.*
import koma.util.coroutine.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.io.File
import java.util.Optional
import kotlin.time.Duration
import kotlin.time.seconds
import koma.util.KResult
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import koma.util.KResult as Result

private val logger = KotlinLogging.logger {}

@Serializable
data class SendResult(
        val event_id: EventId
)

@Serializable
class UpdateAvatarResult()

class MatrixApi internal constructor(
        private val token: String,
        val userId: UserId,
        val server: Server
) {
    private val txnId = atomic(0L)
    private fun getTxnId(): String {
        val t = System.currentTimeMillis()
        val id = txnId.updateAndGet { value ->
            if (t > value) t else value + 1
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

    suspend fun getRoomMessages(roomId: RoomId,
                                /**
                                 * Required.
                                 */
                                from: String,
                                /**
                                 * Required. The direction to return events from. One of: ["b", "f"]
                                 */
                                direction: FetchDirection,
                                limit: Int = 10,
                                /**
                                 * A JSON RoomEventFilter to filter returned events with.
                                 */
                                filter: String? = null,
                                to: String?=null
    ): KResultF<MessageChunks> {
        return request(method = HttpMethod.Get) {
            buildUrl("rooms", roomId.full, "messages")
            parameter("from", from)
            parameter("to", to)
            parameter("dir", direction.toName())
            parameter("limit", limit)
            parameter("filter", filter)
        }
    }

    suspend fun joinRoom(roomid: RoomId): KResultF<JoinRoomResult> {
        return request(method = HttpMethod.Post) {
            buildUrl("rooms", roomid.full, "join")
        }
    }

    suspend fun getEventContext(roomid: RoomId, eventId: EventId, limit: Int = 2): KResultF<ContextResponse> {
        return request(method = HttpMethod.Get) {
            buildUrl("rooms", roomid.full, "context", eventId)
            parameter("limit", limit)
        }
    }

    suspend fun uploadMedia(body: OutgoingContent) : KResultF<UploadResponse> {
        return request(method = HttpMethod.Post) {
            url {
                takeFrom(server.apiUrlKtor)
                path(*server.mediaPathSegments, "upload")
            }
            parameter("access_token", token)
            this.body = body
        }
    }
    suspend fun uploadFile(file: File, contentType: ContentType?=null): KResultF<UploadResponse> {
        val content = if (contentType != null) {
            LocalFileContent(file, contentType)
        } else LocalFileContent(file)
        return uploadMedia(content)
    }
    suspend fun uploadByteArray(contentType: ContentType?=null, byteArray: ByteArray): KResultF<UploadResponse> {
        val content = if (contentType != null) {
            ByteArrayContent(byteArray, contentType)
        } else ByteArrayContent(byteArray)
        return uploadMedia(content)
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

    suspend fun findPublicRooms(query: RoomDirectoryQuery): KResult<RoomListing, KomaFailure>{
        return request(method = HttpMethod.Post) {
            buildUrl("publicRooms")
            jsonBody(query)
        }
    }

    internal val longTimeoutClient
        get() = server.longTimeoutClient
    suspend fun sync(since: String?
                            , timeout: Duration = 50.seconds
                            , full_state: Boolean = false
                     , filter: String? = null,
                     set_presence: UserPresenceType? = null
                            , networkTimeout: Duration = timeout + 10.seconds
    ): Result<SyncResponse, KomaFailure> {
        val (success,failure, result) =withTimeout(networkTimeout) {
            longTimeoutClient.requestResult<SyncResponse>(HttpMethod.Get) {
                buildUrl("sync")
                parameter("since", since)
                parameter("timeout", timeout.toLongMilliseconds())
                parameter("full_state", full_state)
                parameter("filter", filter)
                parameter("set_presence", set_presence)
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