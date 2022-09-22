package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.security.AccessTokenAuthenticator
import fi.elsapalvelu.elsa.service.AuthenticationTokenService
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Qualifier("SisuTre")
@Service
class SisuTreClientBuilderImpl(
    sisuTreAuthenticationTokenService: AuthenticationTokenService,
    applicationProperties: ApplicationProperties
) : OkHttpClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
        Companion.sisuTreAuthenticationTokenService = sisuTreAuthenticationTokenService
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var sisuTreAuthenticationTokenService: AuthenticationTokenService

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "Ocp-Apim-Subscription-Key" to applicationProperties.getSecurity()
                                .getSisuTre().subscriptionKey!!,
                            "Content-Type" to "application/json"
                        )
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
