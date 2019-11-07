package koma.util.coroutine

import koma.KResultF
import koma.Timeout
import koma.util.KResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Clock
import kotlin.time.MonoClock

suspend fun<T> withTimeout(duration: Duration, block: suspend  CoroutineScope.() -> T): KResult<T, Timeout> {
    return coroutineScope {
        val t = async {
            block()
        }
        select<KResult<T, Timeout>> {
            t.onAwait {
                KResult.success(it)
            }
            onTimeout(duration.toLongMilliseconds()) {
                t.cancel()
                KResult.failure(Timeout(duration))
            }
        }
    }
}
