package fi.elsapalvelu.elsa.externalintegration.peppi.oulu

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.impl.PeppiOuluClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.PeppiOuluOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.PeppiOuluOpintotietodataFetchingServiceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [PeppiOuluExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class PeppiOuluExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var peppiOuluOpintotietodataFetchingServiceImpl: PeppiOuluOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var peppiOuluOpintosuorituksetFetchingServiceImpl: PeppiOuluOpintosuorituksetFetchingServiceImpl

    @Autowired
    private lateinit var peppiOuluClientBuilderImpl: PeppiOuluClientBuilderImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = peppiOuluOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = peppiOuluOpintosuorituksetFetchingServiceImpl

    override fun getTestHetu() = "260863-997M"

    @Test
    fun shouldBuildApolloClientWithoutRuntimeLinkageErrors() {
        assertThatCode { peppiOuluClientBuilderImpl.apolloClient() }
            .describedAs("Apollo client creation must not fail because of incompatible runtime dependencies")
            .doesNotThrowAnyException()
    }

    @Test
    fun shouldThrowApolloExceptionWhenUrlIsInvalid() {
        val invalidClientBuilder = object : GraphQLClientBuilder {
            override fun okHttpClient() = OkHttpClient()
            override fun apolloClient(): ApolloClient = ApolloClient.Builder()
                .serverUrl("https://this-host-does-not-exist.invalid/graphql")
                .build()
        }
        val serviceWithInvalidUrl = PeppiOuluOpintotietodataFetchingServiceImpl(
            invalidClientBuilder,
            Mockito.mock(YliopistoRepository::class.java)
        )

        assertThatThrownBy {
            runBlocking { serviceWithInvalidUrl.fetchOpintotietodata("260863-997M") }
        }
            .isInstanceOf(ApolloException::class.java)
            .hasMessageContaining("Failed to execute GraphQL http network request")
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    PeppiOuluClientBuilderImpl::class,
    PeppiOuluOpintotietodataFetchingServiceImpl::class,
    PeppiOuluOpintosuorituksetFetchingServiceImpl::class
)
class PeppiOuluExternalIntegrationTestApplication {
    /** Stub – [YliopistoRepository] is only used by shouldFetch* guards, not by the fetch methods under test. */
    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
