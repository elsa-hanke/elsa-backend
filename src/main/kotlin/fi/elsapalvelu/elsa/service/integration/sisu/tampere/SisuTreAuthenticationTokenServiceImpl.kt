package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import fi.elsapalvelu.elsa.required

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.security.AuthenticationToken
import fi.elsapalvelu.elsa.security.AuthenticationTokenCache
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.kayttaja.AuthenticationTokenService
import okhttp3.FormBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDateTime

private const val TOKEN_PATH = "oauth2/v2.0/token"
private const val ALERT_SUBJECT = "Tampere opintotietointegraation autentikointi epäonnistui"

@Service
class SisuTreAuthenticationTokenServiceImpl(
    @Qualifier("AuthenticationTokenClient") private val sisuTreAuthTokenClientBuilder: OkHttpClientBuilder,
    private val objectMapper: ObjectMapper,
    private val applicationProperties: ApplicationProperties,
    private val integrationAlertService: IntegrationAlertService
) : AuthenticationTokenService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getCachedTokenOrRequestNew(): String? {
        val sisuTreProperties = applicationProperties.getSecurity().getSisuTre()

        AuthenticationTokenCache.getTokenByClientId(sisuTreProperties.clientId.required())?.let {
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
            .add("client_id", sisuTreProperties.clientId.required())
            .add("client_secret", sisuTreProperties.clientSecret.required())
            .build()
        val request = Request.Builder().url(endpointUrl).post(formBody).build()

        try {
            return sisuTreAuthTokenClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string()
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl $responseBody")
                    publishAuthenticationFailureAlert(
                        response.code,
                        endpointUrl,
                        responseBody
                    )
                    return null
                }
                response.body?.string().let { body ->
                    val tokenResponse = objectMapper.readValue(body, TokenResponse::class.java)
                    val accessToken = tokenResponse?.accessToken
                    val expiresIn = tokenResponse?.expiresIn
                    if (accessToken == null || expiresIn == null) {
                        log.error(
                            "Rajapinta $endpointUrl ei palauttanut kelvollista auth tokenia. " +
                                "Token: ${tokenResponse.toString()}"
                        )
                        return null
                    }

                    AuthenticationTokenCache.storeTokenByClientId(
                        sisuTreProperties.clientId.required(), AuthenticationToken(
                            accessToken = accessToken,
                            // Huomioidaan umpeutumisajassa verkkoviive 5 sekuntia.
                            expires = LocalDateTime.now().plusSeconds(expiresIn - 5)
                        )
                    )
                    integrationAlertService.markSuccessful(IntegrationAlertKey.SISU_TRE_OAUTH)
                    accessToken
                }
            }
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
        }
        return null
    }

    private fun publishAuthenticationFailureAlert(
        status: Int,
        endpointUrl: String,
        responseBody: String?
    ) {
        val oauthError = parseOauthError(responseBody)
        val message = buildString {
            append("Tampereen opintotietointegraatio ei saanut OAuth2-tokenia.")
            append(" HTTP status: $status.")
            oauthError?.get("error")?.asText()?.let { append(" Error: $it.") }
            oauthError?.get("error_codes")?.joinToString { it.asText() }?.let {
                append(" Error codes: $it.")
            }
            append(" Endpoint: $endpointUrl.")
            append(" Virhe ei liity yksittäiseen henkilötunnukseen.")
        }
        integrationAlertService.publishOnceUntilSuccess(
            IntegrationAlertKey.SISU_TRE_OAUTH,
            ALERT_SUBJECT,
            message
        )
    }

    private fun parseOauthError(responseBody: String?) =
        if (responseBody.isNullOrBlank()) {
            null
        } else try {
            objectMapper.readTree(responseBody)
        } catch (_: JsonProcessingException) {
            null
        }
}

data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String?,

    @JsonProperty("expires_in")
    val expiresIn: Long?
)
