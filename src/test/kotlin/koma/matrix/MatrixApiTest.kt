package koma.matrix

import koma.Failure
import koma.IOFailure
import koma.OtherFailure
import koma.Server
import koma.matrix.room.naming.RoomId
import koma.network.client.okhttp.KHttpClient
import koma.util.KResult
import koma.util.failureOrThrow
import koma.util.getOr
import koma.util.getOrThrow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalContracts
internal class MatrixApiTest {

    @Test
    fun getRoomName() {
        val server = MockWebServer()
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        runBlocking {
            val n= api.getRoomName(RoomId("r"))
            assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is IOFailure)
            val i = (f as IOFailure)
            assert(i.throwable is SocketTimeoutException)
        }
    }

    private suspend fun returnNonLocal(): KResult<Unit, Failure> {
        val server = MockWebServer()
        @Suppress("BlockingMethodInNonBlockingContext")
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        val nq = api.getRoomName(RoomId("r")) getOrUnstable {
            return KResult.failure(OtherFailure("some failure"))
        }
        error("unexpected success")
    }

    @Test
    fun testInlineCastError() {
        assertDoesNotThrow {
            runBlocking {
                returnNonLocal()
            }
        }
        assertThrows<ClassCastException> {
            val result = runBlocking {
                returnNonLocal()
            }
        }
    }

    @Test
    fun getRoomAvatar() {
        val server = MockWebServer()
        server.start()
        val base = server.url("vx/mock")
        val s = Server(base, KHttpClient.client.newBuilder().readTimeout(1, TimeUnit.MILLISECONDS).build())
        val api = s.account(UserId("u"), "token")
        runBlocking {
            val n = api.getRoomAvatar(RoomId("r"))
              assert(n.isFailure)
            val f = n.failureOrThrow()
            assertTrue(f is IOFailure)
            val i = (f as IOFailure)
            assert(i.throwable is SocketTimeoutException)
        }
    }
}

@ExperimentalContracts
@Suppress("NOTHING_TO_INLINE")
private inline infix fun <R, T : R, E: Any> KResult<T, E>.getOrUnstable( onFailure: (E) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return if (isFailure) {
        onFailure(failureOrThrow())
    } else {
        getOrThrow()
    }
}
