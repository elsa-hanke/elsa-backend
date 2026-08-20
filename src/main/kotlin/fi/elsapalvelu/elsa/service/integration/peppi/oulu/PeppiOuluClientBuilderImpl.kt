package fi.elsapalvelu.elsa.service.integration.peppi.oulu

import fi.elsapalvelu.elsa.required

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.service.integration.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.IntegrationAuthenticationAlertInterceptor
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Qualifier("PeppiOulu")
@Service
class PeppiOuluClientBuilderImpl(
    applicationProperties: ApplicationProperties,
    resourceLoader: ResourceLoader,
    integrationAlertService: IntegrationAlertService
) : GraphQLClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
        Companion.resourceLoader = resourceLoader
        Companion.integrationAlertService = integrationAlertService
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var resourceLoader: ResourceLoader
        private lateinit var integrationAlertService: IntegrationAlertService

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "Token" to applicationProperties.getSecurity().getPeppiOulu().token.required()
                        )
                    )
                )
                .addInterceptor(
                    IntegrationAuthenticationAlertInterceptor(
                        integrationAlertService,
                        IntegrationAlertKey.PEPPI_OULU_AUTHENTICATION,
                        "Oulun Peppi",
                        alertOnNetworkFailure = true,
                        resetOnSuccessfulResponse = false
                    )
                )
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        val apolloClient: ApolloClient by lazy {
            ApolloClient.Builder()
                .serverUrl(applicationProperties.getSecurity().getPeppiOulu().graphqlEndpointUrl.required())
                .okHttpClient(okHttpClient)
                .build()
        }
    }

    override fun apolloClient(): ApolloClient {
        return apolloClient
    }

    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }
}
