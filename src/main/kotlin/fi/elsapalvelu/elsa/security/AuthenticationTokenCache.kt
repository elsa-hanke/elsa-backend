package fi.elsapalvelu.elsa.security

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthenticationTokenCache(private val clock: Clock) {

    private val tokenit: ConcurrentHashMap<String, AuthenticationToken> = ConcurrentHashMap()

    fun getTokenByClientId(clientId: String): AuthenticationToken? {
        return tokenit[clientId]
            ?.takeIf { it.expires.isAfter(LocalDateTime.now(clock)) }
    }

    fun storeTokenByClientId(clientId: String, token: AuthenticationToken) {
        tokenit[clientId] = token
    }
}
