package koma.matrix.user.auth

data class Unauthorized(
        // Need more stages
        val completed: List<String>? = null,
        // Try again, for example, incorrect passwords
        val errcode: String? = null,
        val error: String? = null,
        val flows: List<AuthFlow<String>>,
        val params: Map<String, Any>,
        val session: String? = null
) {
    fun flows(): List<AuthFlow<AuthType>> {
        return flows.map { flow ->
            AuthFlow<AuthType>(flow.stages.map { stage -> AuthType.parse(stage) })
        }
    }
    companion object {
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