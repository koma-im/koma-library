package koma.matrix.pagination

enum class FetchDirection{
    Backward,
    Forward;

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
