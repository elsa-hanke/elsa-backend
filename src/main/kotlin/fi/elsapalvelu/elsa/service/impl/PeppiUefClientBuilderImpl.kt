package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Qualifier("PeppiUef")
@Service
class PeppiUefClientBuilderImpl(applicationProperties: ApplicationProperties
) : OkHttpClientBuilder {
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
                            "X-Api-Key" to applicationProperties.getSecurity().getPeppiUef().apiKey!!
                        )
                    )
                )
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .build()
        }
    }

    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }
}
