package koma.matrix.user.auth

import koma.matrix.UserId
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.http.Body


sealed class RegisterData() {
    // Use empty request to get auth types
    class Query(): RegisterData()
    class Password(
            val username: String,
            val password: String,
            val auth: Map<String, String> = mapOf(Pair("type", "m.login.dummy"))
    ): RegisterData()
}

data class RegisterdUser(
        val access_token: String,
        // Deprecated. Clients should extract the server_name from user_id (by splitting at the first colon)
        val home_server: String? = null,
        val user_id: UserId,
        val refresh_token: String? = null
)

interface MatrixRegisterApi {
    // "_matrix/client/r0/register")
    fun register(@Body data: Any): Call<RegisterdUser>
}

class Register(val server: HttpUrl, httpClient: OkHttpClient) {
    private var session: String? = null
}

