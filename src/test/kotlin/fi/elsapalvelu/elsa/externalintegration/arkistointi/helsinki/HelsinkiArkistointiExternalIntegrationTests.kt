package fi.elsapalvelu.elsa.externalintegration.arkistointi.helsinki

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiDispatcher
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiServiceImpl
import fi.elsapalvelu.elsa.service.arkistointi.louhi.LouhiArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.louhi.TampereLouhiService
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import fi.elsapalvelu.elsa.service.arkistointi.siilo.HelsinkiSiiloService
import fi.elsapalvelu.elsa.service.arkistointi.siilo.SiiloArkistointiAdapter
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.kayttaja.AlertPublisherServiceImpl
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

/**
 * External integration tests for Helsinki archiving (HTTP/Siilo + SÄHKE2 XML).
 *
 * The tests build a real SÄHKE2 package and send it to Helsinki's Siilo test endpoint.
 * The gateway selects the HY test tenant and EL/EHL/YEK database; ELSA supplies the
 * document-type collection ID in the request URL.
 *
 * Required environment variables:
 *  - APPLICATION_ARKISTOINTI_HKI_HOST
 *  - APPLICATION_ARKISTOINTI_HKI_API_KEY
 *  - APPLICATION_ARKISTOINTI_HKI_METADATA_CONTACT_PERSON
 *  - APPLICATION_ARKISTOINTI_HKI_METADATA_CONTACT_ADDRESS
 *  - APPLICATION_ARKISTOINTI_HKI_METADATA_CONTACT_PHONE
 *  - APPLICATION_ARKISTOINTI_HKI_METADATA_CONTACT_EMAIL
 */
@SpringBootTest(classes = [HelsinkiArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class HelsinkiArkistointiExternalIntegrationTests : ExternalIntegrationTestSupport() {

    @Autowired
    private lateinit var arkistointiService: ArkistointiServiceImpl

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
            nimi = YliopistoEnum.HELSINGIN_YLIOPISTO
        )
        val erikoisala = Erikoisala(
            id = 1L,
            nimi = "Yleislääketiede"
        )
        return Opintooikeus(
            id = 9002L,
            opiskelijatunnus = "H123456",
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala
        )
    }

    private fun buildTestAsiakirjat(): List<RecordProperties> {
        val pdfBytes = javaClass.getResourceAsStream("/fixtures/valid.pdf")
            ?.readBytes()
            ?: "%PDF-1.4 mock-content-for-test".toByteArray()
        val data = AsiakirjaData(id = 1L, data = pdfBytes)
        val asiakirja = Asiakirja(
            id = 1L,
            nimi = "yhteenveto_testi_testaaja.pdf",
            tyyppi = "application/pdf",
            asiakirjaData = data
        )

        return listOf(RecordProperties(asiakirja, RecordType.YHTEENVETO))
    }

    @Test
    fun shouldMuodostaSahkeAndCreateZipFile() {
        val opintooikeus = buildTestOpintooikeus()
        val result = muodostaSahke(opintooikeus)

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

    @Test
    fun shouldLahetaSahkeToSiiloWithoutErrors() {
        val result = muodostaSahke(buildTestOpintooikeus())

        log.info("ZIP created at: {}, sending to Siilo...", result.zipFilePath)

        assertThatCode {
            arkistointiService.laheta(
                yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                filePath = result.zipFilePath,
                caseType = CaseType.VALMISTUMINEN,
                yek = false
            )
        }
            .describedAs("laheta must not throw - file transfer to Helsinki Siilo must succeed")
            .doesNotThrowAnyException()

        assertThat(Files.exists(Paths.get(result.zipFilePath)))
            .describedAs("After laheta the local temp ZIP file must have been deleted")
            .isFalse
    }

    private fun muodostaSahke(opintooikeus: Opintooikeus) = arkistointiService.muodostaSahke(
        opintooikeus = opintooikeus,
        asiakirjat = buildTestAsiakirjat(),
        caseId = opintooikeus.id!!.toString(),
        tarkastaja = "Virkailija Virtanen",
        tarkastusPaiva = LocalDate.now().minusDays(2),
        hyvaksyja = "Vastuuhenkilö Väinämöinen",
        hyvaksymisPaiva = LocalDate.now().minusDays(1),
        yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
        caseType = CaseType.VALMISTUMINEN
    )
}

/**
 * Minimal Spring application context for Helsinki Siilo external integration tests.
 * Tampere's SFTP client is mocked because it is not under test here.
 */
@SpringBootConfiguration
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    HelsinkiSiiloService::class,
    LouhiArkistointiAdapter::class,
    SiiloArkistointiAdapter::class,
    ArkistointiConfigurationProvider::class,
    SahkeMetadataBuilder::class,
    SahkePakettiBuilder::class,
    ArkistointiDispatcher::class,
    ArkistointiServiceImpl::class,
    ArkistointiMetricsService::class,
    AlertPublisherServiceImpl::class
)
class HelsinkiArkistointiExternalIntegrationTestApplication {

    @Bean
    fun tampereLouhiService(): TampereLouhiService =
        Mockito.mock(TampereLouhiService::class.java)

    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
