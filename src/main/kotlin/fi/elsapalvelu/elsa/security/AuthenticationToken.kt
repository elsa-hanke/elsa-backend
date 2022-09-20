package fi.elsapalvelu.elsa.security

import java.io.Serializable
import java.time.LocalDateTime

data class AuthenticationToken(

    val accessToken: String,

    val expires: LocalDateTime

) : Serializable {
    override fun toString() = "AuthenticationToken{accessToken=$accessToken, expires=$expires}"
}

