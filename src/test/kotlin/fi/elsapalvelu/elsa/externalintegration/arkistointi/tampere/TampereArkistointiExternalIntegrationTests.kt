package fi.elsapalvelu.elsa.externalintegration.arkistointi.tampere

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.ArkistointiServiceImpl
import fi.elsapalvelu.elsa.service.impl.HelsinkiSiiloService
import fi.elsapalvelu.elsa.service.impl.TampereLouhiService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

/**
 * External integration tests for Tampere arkistointi (SFTP/Louhi + SÄHKE2 XML).
 *
 * Verifies that [ArkistointiServiceImpl.muodostaSahke] can build a valid SÄHKE2 ZIP package
 * and that [ArkistointiServiceImpl.laheta] can deliver it to the real Tampere Louhi test endpoint
 * via SFTP.
 *
 * These tests are intended to run in AWS CodeBuild where:
 *  - The build environment IP is whitelisted on the Tampere Louhi SFTP server.
 *  - The required environment variables are injected (see [TampereArkistointiExternalIntegrationTestApplication] docs).
 *
 * Required env vars:
 *  - APPLICATION_ARKISTOINTI_TRE_HOST
 *  - APPLICATION_ARKISTOINTI_TRE_PORT
 *  - APPLICATION_ARKISTOINTI_TRE_USER
 *  - APPLICATION_ARKISTOINTI_TRE_PRIVATE_KEY_LOCATION
 *  - APPLICATION_ARKISTOINTI_TRE_METADATA_CONTACT_PERSON
 *  - APPLICATION_ARKISTOINTI_TRE_METADATA_CONTACT_ADDRESS
 *  - APPLICATION_ARKISTOINTI_TRE_METADATA_CONTACT_PHONE
 *  - APPLICATION_ARKISTOINTI_TRE_METADATA_CONTACT_EMAIL
 *  - APPLICATION_ARKISTOINTI_TRE_KAYTOSSA (optional, defaults to true)
 */
@SpringBootTest(classes = [TampereArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class TampereArkistointiExternalIntegrationTests : ExternalIntegrationTestSupport() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var arkistointiService: ArkistointiServiceImpl

    // -------------------------------------------------------------------------
    // Test-data helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a minimal [Opintooikeus] object graph with enough data for
     * [ArkistointiServiceImpl.muodostaSahke] to construct a SÄHKE2 package without NPE.
     */
    private fun buildTestOpintooikeus(): Opintooikeus {
        val user = User().apply {
            firstName = "Testi"
            lastName = "Testaaja"
        }
        val kayttaja = Kayttaja(user = user)
        val erikoistuvaLaakari = ErikoistuvaLaakari(
            id = 1L,
            syntymaaika = LocalDate.of(1990, 1, 1),
            kayttaja = kayttaja
        )
        val yliopisto = Yliopisto(
            id = 1L,
            nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO
        )
        val erikoisala = Erikoisala(
            id = 1L,
            nimi = "Yleislääketiede"
        )
        return Opintooikeus(
            id = 9001L,
            opiskelijatunnus = "H123456",
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala
        )
    }

    /**
     * Builds test [RecordProperties] using a minimal PDF byte array.
     * [ArkistointiServiceImpl.muodostaSahke] only needs the raw bytes and the filename –
     * it does not validate PDF content.
     */
    private fun buildTestAsiakirjat(): List<RecordProperties> {
        val pdfBytes: ByteArray = javaClass.getResourceAsStream("/fixtures/valid.pdf")
            ?.readBytes()
            ?: "%PDF-1.4 mock-content-for-test".toByteArray()

        fun makeAsiakirja(id: Long, nimi: String): Asiakirja {
            val data = AsiakirjaData(id = id, data = pdfBytes)
            return Asiakirja(id = id, nimi = nimi, tyyppi = "application/pdf", asiakirjaData = data)
        }

        return listOf(
            RecordProperties(makeAsiakirja(1L, "yhteenveto_testi_testaaja.pdf"), RecordType.YHTEENVETO),
            RecordProperties(makeAsiakirja(2L, "liite_testi_testaaja.pdf"), RecordType.LIITE)
        )
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that [ArkistointiServiceImpl.muodostaSahke] produces a non-empty ZIP file at
     * the returned path and that the file actually exists on the filesystem.
     *
     * This test exercises the entire SÄHKE2 XML serialisation + ZIP packaging pipeline without
     * requiring network connectivity.
     */
    @Test
    fun shouldMuodostaSahkeAndCreateZipFile() {
        val opintooikeus = buildTestOpintooikeus()
        val asiakirjat = buildTestAsiakirjat()

        log.info(
            "Testing muodostaSahke for {}, opintooikeusId={}",
            opintooikeus.yliopisto?.nimi, opintooikeus.id
        )

        val result = arkistointiService.muodostaSahke(
            opintooikeus = opintooikeus,
            asiakirjat = asiakirjat,
            caseId = opintooikeus.id!!.toString(),
            tarkastaja = "Virkailiija Virtanen",
            tarkastusPaiva = LocalDate.now().minusDays(2),
            hyvaksyja = "Vastuuhenkilö Väinämöinen",
            hyvaksymisPaiva = LocalDate.now().minusDays(1),
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN
        )

        log.info("muodostaSahke produced ZIP at: {}", result.zipFilePath)

        assertThat(result.zipFilePath)
            .describedAs("muodostaSahke must return a non-blank ZIP file path")
            .isNotBlank

        val zipPath = Paths.get(result.zipFilePath)
        assertThat(Files.exists(zipPath))
            .describedAs("ZIP file must exist at path: ${result.zipFilePath}")
            .isTrue

        assertThat(Files.size(zipPath))
            .describedAs("ZIP file must not be empty")
            .isGreaterThan(0)
    }

    /**
     * Verifies the full end-to-end archival flow against the real Tampere Louhi test SFTP server:
     *  1. [ArkistointiServiceImpl.muodostaSahke] builds the SÄHKE2 ZIP package.
     *  2. [ArkistointiServiceImpl.laheta] delivers the ZIP to the Louhi InProgress directory and
     *     then moves it to Finished/ELSA.
     *
     * After a successful [laheta] call the local temp file is automatically deleted by
     * [TampereLouhiService], so we also assert it is gone.
     *
     * This test requires the NAT-gateway-whitelisted CodeBuild environment and the SFTP credentials
     * configured via the env vars listed in the class-level KDoc.
     */
    @Test
    fun shouldLahetaSahkeToLouhiWithoutErrors() {
        val opintooikeus = buildTestOpintooikeus()
        val asiakirjat = buildTestAsiakirjat()

        log.info(
            "Testing muodostaSahke + laheta for {}, opintooikeusId={}",
            opintooikeus.yliopisto?.nimi, opintooikeus.id
        )

        val result = arkistointiService.muodostaSahke(
            opintooikeus = opintooikeus,
            asiakirjat = asiakirjat,
            caseId = opintooikeus.id!!.toString(),
            tarkastaja = "Virkailija Virtanen",
            tarkastusPaiva = LocalDate.now().minusDays(2),
            hyvaksyja = "Vastuuhenkilö Väinämöinen",
            hyvaksymisPaiva = LocalDate.now().minusDays(1),
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN
        )

        log.info("ZIP created at: {}, sending to Louhi...", result.zipFilePath)

        assertThatCode {
            arkistointiService.laheta(
                yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
                filePath = result.zipFilePath,
                caseType = CaseType.VALMISTUMINEN,
                yek = false
            )
        }
            .describedAs("laheta must not throw – file transfer to Tampere Louhi must succeed")
            .doesNotThrowAnyException()

        log.info("laheta completed successfully, local temp file cleaned up by TampereLouhiService")

        // TampereLouhiService deletes the local temp file in its finally-block after a successful transfer.
        assertThat(Files.exists(Paths.get(result.zipFilePath)))
            .describedAs("After successful laheta the local temp ZIP file must have been deleted")
            .isFalse
    }
}

// =============================================================================
// Minimal Spring application context for the Tampere arkistointi integration tests
// =============================================================================

/**
 * Slim Spring Boot configuration that wires up only the beans required to test
 * [TampereLouhiService] and [ArkistointiServiceImpl].
 *
 * [HelsinkiSiiloService] is mocked because it is not under test here and would
 * require its own network credentials + HKI SFTP access.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    TampereLouhiService::class,
    ArkistointiServiceImpl::class
)
class TampereArkistointiExternalIntegrationTestApplication {

    /**
     * Mock for [HelsinkiSiiloService] – only TRE archival is under test.
     * The mock satisfies the constructor dependency of [ArkistointiServiceImpl] without
     * requiring HKI credentials or network access.
     */
    @Bean
    fun helsinkiSiiloService(): HelsinkiSiiloService =
        Mockito.mock(HelsinkiSiiloService::class.java)
}

