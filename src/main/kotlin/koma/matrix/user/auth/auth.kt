package koma.matrix.user.auth

import koma.AuthFailure
import koma.Failure
import koma.HttpFailure
import koma.IOFailure
import koma.util.KResult as Result
import koma.matrix.json.MoshiInstance
import koma.util.coroutine.adapter.retrofit.await
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.util.coroutine.adapter.retrofit.extractMatrix
import koma.util.getOr
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call

/**
 * the server may return instructions for further authentication
 */
internal suspend fun <T : Any> Call<T>.awaitMatrixAuth(): Result<T, Failure> {
    val res = this.await() getOr {return Result.failure(it)}
    val body = res.errorBody()
    if (res.code() == 401 && body!=null) {
        val unauth = withContext(Dispatchers.IO) {
            Unauthorized.jsonAdapter.fromJson(body.source())
        }
        if (unauth != null) {
            return Result.failure(AuthFailure(unauth, res.code(), res.message()))
        }
    }
    return res.extractMatrix()
}


data class Unauthorized(
        // Need more stages
        val completed: List<String>?,
        // Try again, for example, incorrect passwords
        val errcode: String?,
        val error: String?,
        val flows: List<AuthFlow<String>>,
        val params: Map<String, Any>,
        val session: String?
) {
    fun flows(): List<AuthFlow<AuthType>> {
        return flows.map { flow ->
            AuthFlow<AuthType>(flow.stages.map { stage -> AuthType.parse(stage) })
        }
    }
    companion object {
        private val moshi = MoshiInstance.moshi
        val jsonAdapter = moshi.adapter(Unauthorized::class.java)
    }
}

data class AuthFlow<T>(
        val stages: List<T>
)

sealed class AuthType(val type: String) {
    class Dummy(t: String): AuthType(type=t)
    class Email(t: String): AuthType(t)
    class Recaptcha(t: String): AuthType(t)
    class Other(type: String): AuthType(type)

    companion object {
        fun parse(s: String): AuthType {
            return when (s) {
                "m.login.dummy" -> Dummy(s)
                "m.login.recaptcha" -> Recaptcha(s)
                "m.login.email.identity" -> Email(s)
                else -> Other(s)
            }
        }
    }

    fun toDisplay(): String {
        return when (this) {
            is Dummy -> "Password"
            is Email -> "Email"
            is Recaptcha -> "Captcha"
            is Other -> this.type
        }
    }
}