package koma.util

fun <T, R> T.given(v: R?, f: T.(R)->T): T {
    return if (v!= null) {
        this.f(v)
    } else {
        this
    }
}
