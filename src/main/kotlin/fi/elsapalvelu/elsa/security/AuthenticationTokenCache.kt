package fi.elsapalvelu.elsa.security

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class AuthenticationTokenCache {

    companion object {
        private val tokens: ConcurrentHashMap<String, AuthenticationToken> = ConcurrentHashMap()

        fun getTokenByClientId(clientId: String): AuthenticationToken? {
            return tokens[clientId]?.takeIf { it.expires > LocalDateTime.now() }
        }

        fun storeTokenByClientId(clientId: String, token: AuthenticationToken) {
            tokens[clientId] = token
        }
    }
}
