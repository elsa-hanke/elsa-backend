package fi.elsapalvelu.elsa.externalintegration.sisu.tre

import com.fasterxml.jackson.databind.DeserializationFeature
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.externalintegration.FetchingServiceExternalIntegrationBase
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.AuthenticationTokenService
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.impl.AuthenticationTokenClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.SisuTreAuthenticationTokenServiceImpl
import fi.elsapalvelu.elsa.service.impl.SisuTreClientBuilderImpl
import fi.elsapalvelu.elsa.service.impl.SisuTreOpintosuorituksetFetchingServiceImpl
import fi.elsapalvelu.elsa.service.impl.SisuTreOpintotietodataFetchingServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * External integration tests for Sisu TRE (REST/JSON with Azure AD OAuth2).
 *
 * Tests [SisuTreOpintotietodataFetchingServiceImpl] and [SisuTreOpintosuorituksetFetchingServiceImpl]
 * as well as the OAuth2 client-credentials flow in [SisuTreAuthenticationTokenServiceImpl]
 * against the real TRE test endpoint.
 *
 * [shouldFetchAccessToken] verifies the authentication layer in isolation; the inherited
 * [@Test] methods cover opintotietodata and opintosuoritukset end-to-end (including auth).
 */
@SpringBootTest(classes = [SisuTreExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class SisuTreExternalIntegrationTests : FetchingServiceExternalIntegrationBase() {

    @Autowired
    private lateinit var sisuTreOpintotietodataFetchingServiceImpl: SisuTreOpintotietodataFetchingServiceImpl

    @Autowired
    private lateinit var sisuTreOpintosuorituksetFetchingServiceImpl: SisuTreOpintosuorituksetFetchingServiceImpl

    override val opintotietodataService: OpintotietodataFetchingService
        get() = sisuTreOpintotietodataFetchingServiceImpl

    override val opintosuorituksetService: OpintosuorituksetFetchingService
        get() = sisuTreOpintosuorituksetFetchingServiceImpl

    /** Direct handle on the token service so we can test auth in isolation. */
    @Autowired
    private lateinit var authenticationTokenService: AuthenticationTokenService

    override fun getTestHetu() = "170999-998Y"

    @Test
    fun shouldFetchAccessToken() {
        val token = authenticationTokenService.requestToken()

        assertThat(token)
            .describedAs("Azure AD OAuth2 access token must not be blank")
            .isNotBlank
    }
}

@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@ImportAutoConfiguration(JacksonAutoConfiguration::class)
@Import(
    AuthenticationTokenClientBuilderImpl::class,
    SisuTreAuthenticationTokenServiceImpl::class,
    SisuTreClientBuilderImpl::class,
    SisuTreOpintotietodataFetchingServiceImpl::class,
    SisuTreOpintosuorituksetFetchingServiceImpl::class
)
class SisuTreExternalIntegrationTestApplication {

    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer {
            it.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }


    @Bean
    fun yliopistoRepository(): YliopistoRepository = Mockito.mock(YliopistoRepository::class.java)
}
