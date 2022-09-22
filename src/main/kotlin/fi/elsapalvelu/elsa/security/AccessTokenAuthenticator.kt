package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.extensions.responseCount
import fi.elsapalvelu.elsa.service.AuthenticationTokenService
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.slf4j.LoggerFactory

class AccessTokenAuthenticator(
    private val authenticationTokenService: AuthenticationTokenService
) : Authenticator {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount() == 1) {
            log.warn(
                "Autentikointi epäonnistui rajapintaan ${response.request.url}. Response: ${response.body}. " +
                    "Haetaan uusi authentication token."
            )
        }
        if (response.responseCount() > 1) {
            log.error("Autentikointi epäonnistui rajapintaan ${response.request.url}. Response: ${response.body}.")
            return null
        }

        synchronized(this) {
            val newToken = authenticationTokenService.requestToken()

            return newToken?.let {
                if (response.request.header("Authorization") != null) {
                    response.request
                        .newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $it")
                        .build()
                } else {
                    response.request
                        .newBuilder()
                        .addHeader("Authorization", "Bearer $it")
                        .build()
                }
            }
        }
    }
}
