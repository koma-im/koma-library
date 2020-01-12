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

internal suspend inline fun <reified T: Any> HttpClient.requestResult(
        method: HttpMethod,
        crossinline block: HttpRequestBuilder.() -> Unit
): KResult<T, KomaFailure> {
   val (success, ex, result) = runCatch {
        this.request<T> {
            this.method = method
            this.block()
        }
    }
    return if (result.testFailure(success, ex)) {
        KResult.failure(ex.toFailure())
    } else {
        KResult.success(success)
    }
}