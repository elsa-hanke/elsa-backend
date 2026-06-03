package fi.elsapalvelu.elsa.config

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.web.filter.CorsFilter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
            callFetchOpintotietodata(
                securityConfiguration(opintotietodataServices = listOf(failingService, successfulService))
            )
        }

        assertThat(result).containsExactly(successfulResult)
        assertThat(failingService.fetchCount).isEqualTo(1)
        assertThat(successfulService.fetchCount).isEqualTo(1)
    }

    @Test
    fun `opintosuoritukset fetch keeps successful integration result when another integration throws`() {
        // CountDownLatch ensures the test waits for both background coroutines to
        // finish (success or failure) before making assertions.  Without this the
        // fire-and-forget coroutines may not have run yet when we assert.
        val latch = CountDownLatch(2)

        val failingService = TestOpintosuorituksetFetchingService(
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            exception = IllegalStateException("token service down"),
            latch = latch
        )
        val successfulService = TestOpintosuorituksetFetchingService(
            yliopisto = YliopistoEnum.ITA_SUOMEN_YLIOPISTO,
            result = OpintosuorituksetPersistenceDTO(YliopistoEnum.ITA_SUOMEN_YLIOPISTO, emptyList()),
            latch = latch
        )

        val persistenceService = mock(OpintosuorituksetPersistenceService::class.java)
        val config = securityConfiguration(
            opintosuorituksetServices = listOf(failingService, successfulService),
            opintosuorituksetPersistenceService = persistenceService
        )

        // The method returns immediately before coroutines complete
        callFetchAndHandleOpintosuorituksetNonBlocking(config, "user-123", "010101-123N")

        // Block until both coroutines have run (or timeout after 5 s)
        assertThat(latch.await(5, TimeUnit.SECONDS))
            .withFailMessage("Timed out waiting for opintosuoritukset coroutines to complete")
            .isTrue()

        // Both university services were attempted
        assertThat(failingService.fetchCount).isEqualTo(1)
        assertThat(successfulService.fetchCount).isEqualTo(1)

        // Only the successful service's result reached the persistence layer
        verify(persistenceService, times(1)).createOrUpdateIfChanged(any(), any())
    }


    @Suppress("UNCHECKED_CAST")
    private suspend fun callFetchOpintotietodata(
        config: SecurityConfiguration
    ): List<OpintotietodataDTO> {
        val fn = SecurityConfiguration::class.declaredFunctions
            .single { it.name == "fetchOpintotietodata" && it.parameters.size == 2 }
        fn.isAccessible = true
        return fn.callSuspend(config, "010101-123N") as List<OpintotietodataDTO>
    }

    private fun callFetchAndHandleOpintosuorituksetNonBlocking(
        config: SecurityConfiguration,
        userId: String,
        hetu: String
    ) {
        val fn = SecurityConfiguration::class.declaredFunctions
            .single { it.name == "fetchAndHandleOpintosuorituksetNonBlocking" }
        fn.isAccessible = true
        fn.call(config, userId, hetu)
    }

    private fun securityConfiguration(
        opintotietodataServices: List<OpintotietodataFetchingService> = emptyList(),
        opintosuorituksetServices: List<OpintosuorituksetFetchingService> = emptyList(),
        opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService =
            mock(OpintosuorituksetPersistenceService::class.java)
    ) = SecurityConfiguration(
        mock(CorsFilter::class.java),
        ApplicationProperties(),
        mock(UserService::class.java),
        mock(OpintooikeusService::class.java),
        opintotietodataServices,
        mock(OpintotietodataPersistenceService::class.java),
        opintosuorituksetServices,
        opintosuorituksetPersistenceService,
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
            fetchCount++
            exception?.let { throw it }
            return result
        }

        override fun shouldFetchOpintotietodata() = true
        override fun getYliopisto() = yliopisto
    }

    private class TestOpintosuorituksetFetchingService(
        private val yliopisto: YliopistoEnum,
        private val result: OpintosuorituksetPersistenceDTO? = null,
        private val exception: Exception? = null,
        private val latch: CountDownLatch
    ) : OpintosuorituksetFetchingService {

        var fetchCount = 0

        override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
            fetchCount++
            try {
                exception?.let { throw it }
                return result
            } finally {
                latch.countDown()
            }
        }

        override fun shouldFetchOpintosuoritukset() = true
        override fun getYliopisto() = yliopisto
    }
}

