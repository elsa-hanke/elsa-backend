package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Qualifier("PeppiOulu")
@Service
class PeppiOuluClientBuilderImpl(
    applicationProperties: ApplicationProperties,
    resourceLoader: ResourceLoader
) : GraphQLClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
        Companion.resourceLoader = resourceLoader
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var resourceLoader: ResourceLoader

        val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "Token" to applicationProperties.getSecurity().getPeppiOulu().token!!
                        )
                    )
                )
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
        }

        val apolloClient: ApolloClient by lazy {
            ApolloClient.Builder()
                .serverUrl(applicationProperties.getSecurity().getPeppiOulu().graphqlEndpointUrl!!)
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

