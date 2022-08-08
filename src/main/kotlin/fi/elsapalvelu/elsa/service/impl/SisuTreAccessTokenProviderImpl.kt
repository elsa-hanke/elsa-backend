package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.SisuTreAccessTokenProvider
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import okhttp3.FormBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

private const val TOKEN_PATH = "oauth2/v2.0/token"

@Service
class SisuTreAccessTokenProviderImpl(
    @Qualifier("SisuTreAccessTokenClient") private val sisuTreAccessTokenClientBuilder: OkHttpClientBuilder,
    private val objectMapper: ObjectMapper,
    private val applicationProperties: ApplicationProperties
) : SisuTreAccessTokenProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun token(): String? {
        val sisuTreProperties = applicationProperties.getSecurity().getSisuTre()
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
            return sisuTreAccessTokenClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, TokenResponse::class.java)?.accessToken
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
}

data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String?
)
