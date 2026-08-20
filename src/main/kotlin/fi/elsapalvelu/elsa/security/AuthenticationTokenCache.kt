package fi.elsapalvelu.elsa.security

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

object AuthenticationTokenCache {

    private val tokens: ConcurrentHashMap<String, AuthenticationToken> = ConcurrentHashMap()

    fun getTokenByClientId(clientId: String): AuthenticationToken? {
        return tokens[clientId]
            ?.takeIf { it.expires.isAfter(LocalDateTime.now()) }
    }

    fun storeTokenByClientId(clientId: String, token: AuthenticationToken) {
        tokens[clientId] = token
    }
}
