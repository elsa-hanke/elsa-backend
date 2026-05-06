package fi.elsapalvelu.elsa.externalintegration

import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Abstract base for external integration tests that verify [OpintotietodataFetchingService] and
 * [OpintosuorituksetFetchingService] implementations against real (test-environment) endpoints.
 *
 * Each concrete subclass provides its own minimal Spring context via
 * `@SpringBootTest(classes = [...])` and overrides the two service properties with `@Autowired`.
 * JUnit 5 discovers the `@Test` methods through the class hierarchy, so no boilerplate test
 * duplication is needed in the subclasses.
 *
 * Goal: ensure that network connectivity, authentication, and JSON/GraphQL deserialization all
 * continue to work correctly when dependencies are upgraded.
 */
abstract class FetchingServiceExternalIntegrationBase : ExternalIntegrationTestSupport() {

    /** Provided by the concrete test class via `@Autowired override lateinit var`. */
    protected abstract val opintotietodataService: OpintotietodataFetchingService

    /** Provided by the concrete test class via `@Autowired override lateinit var`. */
    protected abstract val opintosuorituksetService: OpintosuorituksetFetchingService

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Verifies that [OpintotietodataFetchingService.fetchOpintotietodata] can reach the external
     * endpoint, authenticate, and deserialize the response into an [OpintotietodataDTO] without
     * errors. Also validates that every returned opintooikeus belongs to the correct yliopisto.
     */
    @Test
    fun shouldFetchOpintotietodataWithoutErrors() {
        val yliopisto = opintotietodataService.getYliopisto()
        log.info("Testing fetchOpintotietodata for {}", yliopisto)

        val result = runBlocking { opintotietodataService.fetchOpintotietodata(HETU) }

        log.info(
            "fetchOpintotietodata result for {}: syntymaaika={}, opintooikeusCount={}",
            yliopisto, result?.syntymaaika, result?.opintooikeudet?.size
        )
        assertValidOpintotietodata(result)
        result!!.opintooikeudet?.forEach { oikeus ->
            assertThat(oikeus.yliopisto)
                .describedAs("Every opintooikeus must belong to the correct yliopisto")
                .isEqualTo(yliopisto)
        }
    }

    /**
     * Verifies that [OpintosuorituksetFetchingService.fetchOpintosuoritukset] can reach the
     * external endpoint, authenticate, and deserialize the response without errors.
     * The items list may legitimately be empty if the test person has no suoritukset in that
     * system, but the DTO itself must never be null (null ⇒ exception was swallowed).
     */
    @Test
    fun shouldFetchOpintosuorituksetWithoutErrors() {
        val yliopisto = opintosuorituksetService.getYliopisto()
        log.info("Testing fetchOpintosuoritukset for {}", yliopisto)

        val result = runBlocking { opintosuorituksetService.fetchOpintosuoritukset(HETU) }

        log.info(
            "fetchOpintosuoritukset result for {}: yliopisto={}, itemCount={}",
            yliopisto, result?.yliopisto, result?.items?.size
        )
        assertValidOpintosuoritukset(result)
        assertThat(result!!.yliopisto)
            .describedAs("Returned yliopisto must match the service's declared yliopisto")
            .isEqualTo(yliopisto)
    }
}

