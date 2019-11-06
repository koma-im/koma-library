package koma.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ResultKtTest {

    @Test
    fun r() {
    }

    @Test
    fun isFailure() {
        fun resultPair(): Pair<String?, String?> {
            return "value" to null
        }
        val (v, e) = resultPair()

    }
}