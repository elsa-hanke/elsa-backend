package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.repository.VerificationTokenRepository
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetPersistenceService
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintotietodataPersistenceService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.web.filter.CorsFilter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

class SecurityConfigurationTest {

    @Test
    fun `opintotietodata fetch keeps successful integration result when another integration throws`() {
        val successfulResult = OpintotietodataDTO()
        val failingService = TestOpintotietodataFetchingService(
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            exception = IllegalStateException("token service down")
        )
        val successfulService = TestOpintotietodataFetchingService(
            yliopisto = YliopistoEnum.ITA_SUOMEN_YLIOPISTO,
            result = successfulResult
        )

        val result = runBlocking {
            fetchOpintotietodata(
                securityConfiguration(listOf(failingService, successfulService))
            )
        }

        assertThat(result).containsExactly(successfulResult)
        assertThat(failingService.fetchCount).isEqualTo(1)
        assertThat(successfulService.fetchCount).isEqualTo(1)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun fetchOpintotietodata(
        securityConfiguration: SecurityConfiguration
    ): List<OpintotietodataDTO> {
        val fetchFunction = SecurityConfiguration::class.declaredFunctions
            .single { it.name == "fetchOpintotietodata" && it.parameters.size == 2 }
        fetchFunction.isAccessible = true

        return fetchFunction.callSuspend(securityConfiguration, "010101-123N") as List<OpintotietodataDTO>
    }

    private fun securityConfiguration(
        fetchingServices: List<OpintotietodataFetchingService>
    ) = SecurityConfiguration(
        mock(CorsFilter::class.java),
        ApplicationProperties(),
        mock(UserService::class.java),
        mock(OpintooikeusService::class.java),
        fetchingServices,
        mock(OpintotietodataPersistenceService::class.java),
        emptyList<OpintosuorituksetFetchingService>(),
        mock(OpintosuorituksetPersistenceService::class.java),
        mock(VerificationTokenRepository::class.java),
        mock(OpintooikeusRepository::class.java),
        mock(KayttajaRepository::class.java),
        mock(UserRepository::class.java),
        mock(KouluttajavaltuutusRepository::class.java),
        mock(Environment::class.java),
        mock(ApplicationContext::class.java)
    )

    private class TestOpintotietodataFetchingService(
        private val yliopisto: YliopistoEnum,
        private val result: OpintotietodataDTO? = null,
        private val exception: Exception? = null
    ) : OpintotietodataFetchingService {

        var fetchCount = 0

        override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
            fetchCount += 1
            exception?.let { throw it }

            return result
        }

        override fun shouldFetchOpintotietodata(): Boolean = true

        override fun getYliopisto(): YliopistoEnum = yliopisto
    }
}
