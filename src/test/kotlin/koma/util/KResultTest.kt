package koma.util

import org.junit.Test

internal class KResultTest {
    @Test
    fun testIsFailure() {
        val a: String? = null
        val b: String? = null
        val r = KResult<String, String>("")
        r.testFailure(a, b)
    }
}