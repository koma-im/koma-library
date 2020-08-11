package koma

import io.ktor.http.HttpStatusCode
import koma.matrix.json.jsonDefault
import koma.util.coroutine.adapter.retrofit.toMatrixFailure
import kotlinx.serialization.json.JsonObjectSerializer
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MatrixFailureTest {

    @Test
    fun testToString() {
        val httpStatusCode = HttpStatusCode(200, "OK")
        val s1 = """{
  "errcode": "M_LIMIT_EXCEEDED",
  "error": "Too many requests",
  "retry_after_ms": 2000
}"""
        val o1 = jsonDefault.decodeFromString(JsonObjectSerializer, s1)
        val m = o1.toMatrixFailure(httpStatusCode, s1)
        assertEquals("MatrixFailure(M_LIMIT_EXCEEDED: Too many requests, retry_after_ms: 2000)", m.toString())
        val s2 = """
{
  "errcode": "M_FORBIDDEN",
  "error": "Invalid password",
  "completed": [ "example.type.foo" ],
  "flows": [
    {
      "stages": [ "example.type.foo", "example.type.bar" ]
    },
    {
      "stages": [ "example.type.foo", "example.type.baz" ]
    }
  ],
  "params": {
      "example.type.baz": {
          "example_key": "foobar"
      }
  },
  "session": "xxxxxx"
}"""
        val o2 = jsonDefault .decodeFromString(JsonObjectSerializer, s2)
        val m2 = o2.toMatrixFailure(httpStatusCode, s2)
        assertEquals("MatrixFailure(M_FORBIDDEN: Invalid password, session: xxxxxx)", m2.toString())
    }
}