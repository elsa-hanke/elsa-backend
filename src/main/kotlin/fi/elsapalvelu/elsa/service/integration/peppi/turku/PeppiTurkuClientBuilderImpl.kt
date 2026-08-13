package fi.elsapalvelu.elsa.service.integration.peppi.turku

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.IntegrationAuthenticationAlertInterceptor
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Qualifier("PeppiTurku")
@Service
class PeppiTurkuClientBuilderImpl(
    applicationProperties: ApplicationProperties,
    integrationAlertService: IntegrationAlertService
) : OkHttpClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
        Companion.integrationAlertService = integrationAlertService
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var integrationAlertService: IntegrationAlertService

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "Accept" to "application/json",
                            "ESP-ScreenName" to "peppi_elsa",
                            "username" to "peppi_elsa",
                            "X-Api-Key" to applicationProperties.getSecurity().getPeppiTurku().apiKey.required(),
                            "Authorization" to "Basic ${
                                applicationProperties.getSecurity().getPeppiTurku().basicAuthEncodedKey.required()
                            }"
                        )
                    )
                )
                .addInterceptor(
                    IntegrationAuthenticationAlertInterceptor(
                        integrationAlertService,
                        IntegrationAlertKey.PEPPI_TURKU_AUTHENTICATION,
                        "Turun Peppi",
                        alertOnNetworkFailure = true
                    )
                )
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
        }
    }

    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }
}
