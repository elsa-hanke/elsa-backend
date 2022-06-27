package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.service.PeppiTurkuClientBuilder
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PeppiTurkuClientBuilderImpl(
    applicationProperties: ApplicationProperties
) : PeppiTurkuClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "Accept" to "application/json",
                            "ESP-ScreenName" to "peppi_elsa",
                            "username" to "peppi_elsa",
                            "X-Api-Key" to applicationProperties.getSecurity().getPeppiTurku().apiKey!!,
                            "Authorization" to "Basic ${
                                applicationProperties.getSecurity().getPeppiTurku().basicAuthEncodedKey!!
                            }"
                        )
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
