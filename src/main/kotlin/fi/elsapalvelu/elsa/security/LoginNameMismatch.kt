package fi.elsapalvelu.elsa.security

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.time.Instant

const val LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE = "elsa.loginNameMismatch"

data class LoginNameMismatch(
    val invitationName: String,
    val authenticatedName: String,
    @get:JsonIgnore
    val expiresAt: Instant = Instant.now().plusSeconds(300)
) : Serializable
