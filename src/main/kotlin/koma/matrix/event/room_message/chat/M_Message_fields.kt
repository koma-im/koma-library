package koma.matrix.event.room_message.chat

import kotlinx.serialization.Serializable

@Serializable
class LocationInfo(
        val thumbnail_url: String? = null,
        val thumbnail_info: ThumbnailInfo? = null
)

@Serializable
class ImageInfo(
        val h:Int,
        val w: Int,
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String? = null,
        val thumbnail_info: ThumbnailInfo? =null
)

@Serializable
class AudioInfo(
        val duration: Int? = null,
        val mimetype: String,
        val size: Int
)

@Serializable
class VideoInfo(
        val h:Int,
        val w: Int,
        val duration: Int? = null,
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String? = null,
        val thumbnail_info: ThumbnailInfo? = null
)

@Serializable
class FileInfo(
        val mimetype: String,
        val size: Long,
        val thumbnail_url: String?=null,
        val thumbnail_info: ThumbnailInfo?=null
)

@Serializable
class ThumbnailInfo(
        val h: Int,
        val w: Int,
        val mimetype: String,
        val size: Int
)

@Serializable
class MessageUnsigned(
        /**
         * not used and it keeps changing
         */
        //val age: Int?,
        val transactionId: String? = null
)
