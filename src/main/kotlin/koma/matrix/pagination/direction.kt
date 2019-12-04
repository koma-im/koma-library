package koma.matrix.pagination

import com.squareup.moshi.Json

enum class FetchDirection{
    @Json(name = "b") Backward,
    @Json(name = "f") Forward;

    @Deprecated("ambiguous", ReplaceWith("toName()"))
    override fun toString() = toName()

    /**
     * unquoted
     */
    fun toName() = when(this) {
        Backward -> "b"
        Forward -> "f"
    }
}
