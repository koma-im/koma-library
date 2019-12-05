package koma.util

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import koma.Failure
import koma.KomaFailure
import koma.matrix.user.identity.DisplayName
import koma.toFailure

typealias ContentType = ContentType

fun <T, R> T.given(v: R?, f: T.(R)->T): T {
    return if (v!= null) {
        this.f(v)
    } else {
        this
    }
}

internal suspend inline fun <reified T> HttpClient.requestResult(
        method: HttpMethod,
        crossinline block: HttpRequestBuilder.() -> Unit
): KResult<T, KomaFailure> {
    return runCatch {
        this.request<T> {
            this.method = method
            this.block()
        }
    }.mapFailure {
        it.toFailure()
    }
}