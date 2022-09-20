package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.security.AuthenticationToken
import fi.elsapalvelu.elsa.security.AuthenticationTokenCache
import fi.elsapalvelu.elsa.service.AuthenticationTokenService
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import okhttp3.FormBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDateTime

private const val TOKEN_PATH = "oauth2/v2.0/token"

@Service
class SisuTreAuthenticationTokenServiceImpl(
    @Qualifier("AuthenticationTokenClient") private val sisuTreAuthTokenClientBuilder: OkHttpClientBuilder,
    private val objectMapper: ObjectMapper,
    private val applicationProperties: ApplicationProperties
) : AuthenticationTokenService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getCachedTokenOrRequestNew(): String? {
        val sisuTreProperties = applicationProperties.getSecurity().getSisuTre()

        AuthenticationTokenCache.getTokenByClientId(sisuTreProperties.clientId!!)?.let {
            return it.accessToken
        }

        return requestToken(sisuTreProperties)
    }

    override fun requestToken(): String? {
        val sisuTreProperties = applicationProperties.getSecurity().getSisuTre()
        return requestToken(sisuTreProperties)
    }

    private fun requestToken(
        sisuTreProperties: ApplicationProperties.Security.SisuTre
    ): String? {
        val endpointUrl =
            "${sisuTreProperties.tokenEndpointUrl}/${sisuTreProperties.tenantId}/$TOKEN_PATH"
        val formBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", "api://${sisuTreProperties.scopeId}/.default")
            .add("client_id", sisuTreProperties.clientId!!)
            .add("client_secret", sisuTreProperties.clientSecret!!)
            .build()
        val request = Request.Builder().url(endpointUrl).post(formBody).build()

        try {
            return sisuTreAuthTokenClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body}")
                    return null
                }
                response.body?.string().let { body ->
                    val tokenResponse = objectMapper.readValue(body, TokenResponse::class.java)
                    validateTokenResponse(tokenResponse, sisuTreProperties.clientId!!)

                    AuthenticationTokenCache.storeTokenByClientId(
                        sisuTreProperties.clientId!!, AuthenticationToken(
                            accessToken = tokenResponse.accessToken!!,
                            // Huomioidaan umpeutumisajassa verkkoviive 5 sekuntia.
                            expires = LocalDateTime.now().plusSeconds(tokenResponse.expiresIn!! - 5)
                        )
                    )
                    tokenResponse.accessToken
                }
            }
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonMappingException) {
            log.error(
                "$JSON_MAPPING_ERROR: $endpointUrl ${e.message} "
            )
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
        }
        return null
    }

    private fun validateTokenResponse(tokenResponse: TokenResponse?, endpointUrl: String) {
        if (tokenResponse?.accessToken == null || tokenResponse.expiresIn == null) {
            log.error("Rajapinta $endpointUrl ei palauttanut kelvollista auth tokenia. " +
                "Token: ${tokenResponse.toString()}")
        }
    }
}

data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String?,

    @JsonProperty("expires_in")
    val expiresIn: Long?
)
