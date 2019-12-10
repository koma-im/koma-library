package koma.matrix.event.room_message.chat

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.*

@Serializable(with = MMessageSerializer::class)
sealed class M_Message (
) {
    abstract val body: String
    override fun toString(): String {
        return "[m_message: $body]"
    }
}

@Serializer(forClass = M_Message::class)
internal object MMessageSerializer: KSerializer<M_Message> {
    override val descriptor: SerialDescriptor =
            StringDescriptor.withName("M_Message")

    override fun serialize(encoder: Encoder, obj: M_Message) {
        val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json, not $encoder")
        val tree = when (obj) {
            is TextMessage-> output.json.toJson(TextMessage.serializer(), obj)
            is EmoteMessage-> output.json.toJson(EmoteMessage.serializer(), obj)
            is NoticeMessage-> output.json.toJson(NoticeMessage.serializer(), obj)
            is ImageMessage-> output.json.toJson(ImageMessage.serializer(), obj)
            is FileMessage-> output.json.toJson(FileMessage.serializer(), obj)
            is LocationMessage-> output.json.toJson(LocationMessage.serializer(), obj)
            is VideoMessage-> output.json.toJson(VideoMessage.serializer(), obj)
            is AudioMessage-> output.json.toJson(AudioMessage.serializer(), obj)
            is UnrecognizedMessage -> obj.raw as JsonElement
        }
        output.encodeJson(tree)
    }

    override fun deserialize(decoder: Decoder): M_Message {
        val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
        val msgtype = tree.getPrimitiveOrNull("msgtype")?.content
        return when(msgtype) {
            "m.text" -> input.json.fromJson(TextMessage.serializer(), tree)
            "m.emote" -> input.json.fromJson(EmoteMessage.serializer(), tree)
            "m.notice" -> input.json.fromJson(NoticeMessage.serializer(), tree)
            "m.image" -> input.json.fromJson(ImageMessage.serializer(), tree)
            "m.file" -> input.json.fromJson(FileMessage.serializer(), tree)
            "m.location" -> input.json.fromJson(LocationMessage.serializer(), tree)
            "m.video" -> input.json.fromJson(VideoMessage.serializer(), tree)
            "m.audio" -> input.json.fromJson(AudioMessage.serializer(), tree)
            else -> UnrecognizedMessage(raw = tree, body = tree.getPrimitiveOrNull("body")?.content.toString())
        }
    }
}

fun M_Message.getMsgType(): String? {
    val k = when (this) {
        is TextMessage->   "m.text"
        is EmoteMessage->"m.emote"
        is NoticeMessage->"m.notice"
        is ImageMessage->"m.image"
        is FileMessage->  "m.file"
        is LocationMessage->"m.location"
        is VideoMessage->"m.video"
        is AudioMessage->"m.audio"
        is UnrecognizedMessage -> return null
    }
    return  k
}

@Serializable
@SerialName( "m.text")
class TextMessage(
        override val body: String,
        val formatted_body: String? = null,
        val msgtype: String = "m.text",
        val format: String? = null
) : M_Message() {
    override fun toString(): String {
        return "TextMessage(body='$body', formatted_body=$formatted_body, msgtype='$msgtype', format=$format)"
    }
}

@Serializable
@SerialName("m.emote")
class EmoteMessage(
        override val body: String,
        val msgtype: String = "m.emote"
) : M_Message()

@Serializable
class NoticeMessage(
        val msgtype: String = "m.notice",
        override val body: String
) : M_Message() {
    override fun toString(): String {
        return "NoticeMessage(msgtype='$msgtype', body='$body')"
    }
}

@Serializable
class VideoMessage(
        override val body: String,
        val msgtype: String = "m.video",
        val url: String,
        val info: VideoInfo? = null
) : M_Message()

@Serializable
class AudioMessage(
        override val body: String,
        val msgtype: String = "m.audio",
        val url: String,
        val info: VideoInfo? = null
) : M_Message()

@Serializable
class ImageMessage(
        override val body: String,
        val url: String,
        val info: ImageInfo? = null,
        val msgtype: String = "m.image"
) : M_Message()

@Serializable
class LocationMessage(
        val geo_uri: String,
        val msgtype: String = "m.location",
        val info: LocationInfo? = null,
        override val body: String
) : M_Message()

@Serializable
class FileMessage(
        val filename: String, val url: String, val info: FileInfo? = null,
        val msgtype: String = "m.file",
        override val body: String
) : M_Message()

class UnrecognizedMessage(
        val raw: Map<String, Any>,
        override val body: String = "other"
): M_Message() {
    override fun toString() = "UnrecognizedMessage-$raw"
}
