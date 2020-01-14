package koma.matrix

import koma.matrix.json.jsonDefault
import koma.matrix.json.jsonPretty
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.stringify
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RegistrationDataTest {

    @Test
    fun RegistrationDataTest() {
        val s1 = """{
  "auth": {
    "type": "example.type.foo",
    "session": "xxxxx",
    "example_credential": "verypoorsharedsecret"
  },
  "username": "cheeky_monkey",
  "password": "ilovebananas",
  "device_id": "GHTYAJCE",
  "initial_device_display_name": "Jungle Phone",
  "inhibit_login": false
}"""
        val r1 = jsonDefault.parse(RegistrationData.serializer(), s1)
        assertNotNull(r1.auth)
        assertNotNull(r1.initial_device_display_name)
        assertNotNull(r1.username)
        assertNotNull(r1.auth?.type)
        assertNotNull(r1.auth?.session)
        assertEquals("""{
  "password": "ilovebananas",
  "username": "cheeky_monkey",
  "device_id": "GHTYAJCE",
  "initial_device_display_name": "Jungle Phone",
  "auth": {
    "type": "example.type.foo",
    "session": "xxxxx"
  }
}""", jsonPretty.stringify(RegistrationData.serializer(), r1))
        val r2 = RegistrationData(password = "password", auth = AuthenticationData(type = "dummy"))
        val s2 = jsonDefault.stringify(RegistrationData.serializer(), r2)
        assertEquals("""{"password":"password","auth":{"type":"dummy","session":null}}""", s2)
    }
}