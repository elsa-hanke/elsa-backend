package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.security.AccessTokenAuthenticator
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.IntegrationAuthenticationAlertInterceptor
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.kayttaja.AuthenticationTokenService
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Qualifier("SisuTre")
@Service
class SisuTreClientBuilderImpl(
    sisuTreAuthenticationTokenService: AuthenticationTokenService,
    applicationProperties: ApplicationProperties,
    integrationAlertService: IntegrationAlertService
) : OkHttpClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
        Companion.sisuTreAuthenticationTokenService = sisuTreAuthenticationTokenService
        Companion.integrationAlertService = integrationAlertService
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var sisuTreAuthenticationTokenService: AuthenticationTokenService
        private lateinit var integrationAlertService: IntegrationAlertService

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "Ocp-Apim-Subscription-Key" to applicationProperties.getSecurity()
                                .getSisuTre().subscriptionKey.required(),
                            "Content-Type" to "application/json"
                        )
                    )
                )
                .addInterceptor(
                    IntegrationAuthenticationAlertInterceptor(
                        integrationAlertService,
                        IntegrationAlertKey.SISU_TRE_API_AUTHENTICATION,
                        "Tampereen Sisu",
                        authenticationFailureStatuses = setOf(401, 403),
                        suppressedByAlertKey = IntegrationAlertKey.SISU_TRE_OAUTH
                    )
                )
                .authenticator(AccessTokenAuthenticator(sisuTreAuthenticationTokenService))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
        }
    }

    override fun okHttpClient(): OkHttpClient {
        return okHttpClient.newBuilder().addInterceptor(
            OkHttp3RequestInterceptor(
                mapOf("Authorization" to "Bearer ${sisuTreAuthenticationTokenService.getCachedTokenOrRequestNew()}")
            )
        ).build()
    }
}
