package fi.elsapalvelu.elsa.service.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.AsiakirjaData
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.AlertPublisherService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import java.util.zip.ZipInputStream

@ExtendWith(MockitoExtension::class)
class ArkistointiServiceImplTest {

    @Mock
    private lateinit var tampereLouhiService: TampereLouhiService

    @Mock
    private lateinit var helsinkiSiiloService: HelsinkiSiiloService

    @Mock
    private lateinit var alertPublisherService: AlertPublisherService

    private lateinit var metricsRegistry: SimpleMeterRegistry
    private lateinit var arkistointiMetrics: ArkistointiMetricsService
    private lateinit var lahetaService: ArkistointiServiceImpl

    @BeforeEach
    fun setUp() {
        metricsRegistry = SimpleMeterRegistry()
        arkistointiMetrics = ArkistointiMetricsService(metricsRegistry)
        lahetaService = ArkistointiServiceImpl(
            applicationProperties = ApplicationProperties(),
            tampereLouhiService = tampereLouhiService,
            helsinkiSiiloService = helsinkiSiiloService,
            alertPublisherService = alertPublisherService,
            arkistointiMetrics = arkistointiMetrics
        )
    }

    @Test
    fun `muodostaSahke writes xml into zip when zipMetadata is enabled`() {
        val service = createService(zipMetadata = true)
        val opintooikeus = createOpintooikeus()
        val recordProperties = listOf(createRecordProperties(opintooikeus))

        val result = service.muodostaSahke(
            opintooikeus = opintooikeus,
            asiakirjat = recordProperties,
            caseId = "CASE-123",
            tarkastaja = "Testi Tarkastaja",
            tarkastusPaiva = LocalDate.of(2024, 1, 2),
            hyvaksyja = "Testi Hyvaksyja",
            hyvaksymisPaiva = LocalDate.of(2024, 1, 3),
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN
        )

        try {
            assertNull(result.metadataBytes)
            val entries = readZipEntries(result.zipFilePath)
            assertTrue(entries.containsKey("sahke.xml"))
            assertTrue(entries.containsKey("pdf/testi.pdf"))

            val xml = String(entries.getValue("sahke.xml"), StandardCharsets.UTF_8)
            assertXmlContent(xml)
        } finally {
            Files.deleteIfExists(File(result.zipFilePath).toPath())
        }
    }

    @Test
    fun `muodostaSahke returns metadata bytes when zipMetadata is disabled`() {
        val service = createService(zipMetadata = false)
        val opintooikeus = createOpintooikeus()
        val recordProperties = listOf(createRecordProperties(opintooikeus))

        val result = service.muodostaSahke(
            opintooikeus = opintooikeus,
            asiakirjat = recordProperties,
            caseId = "CASE-123",
            tarkastaja = "Testi Tarkastaja",
            tarkastusPaiva = LocalDate.of(2024, 1, 2),
            hyvaksyja = "Testi Hyvaksyja",
            hyvaksymisPaiva = LocalDate.of(2024, 1, 3),
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN
        )

        try {
            assertNotNull(result.metadataBytes)
            val entries = readZipEntries(result.zipFilePath)
            assertFalse(entries.containsKey("sahke.xml"))
            assertTrue(entries.containsKey("pdf/testi.pdf"))

            val xml = String(result.metadataBytes!!, StandardCharsets.UTF_8)
            assertXmlContent(xml)
        } finally {
            Files.deleteIfExists(File(result.zipFilePath).toPath())
        }
    }

    @Test
    fun `laheta success increments success counter, does not publish alert, and resets active gauge`() {
        lahetaService.laheta(
            yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
            filePath = "/tmp/ok.zip",
            caseType = CaseType.VALMISTUMINEN,
            yek = false
        )

        verify(alertPublisherService, never()).publishAlert(any(), any())

        assertThat(successCount(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.VALMISTUMINEN)).isEqualTo(1.0)
        assertThat(errorCount(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.VALMISTUMINEN)).isEqualTo(0.0)
        assertThat(arkistointiMetrics.activeArkistointiOperations.get()).isEqualTo(0)
    }

    @Test
    fun `laheta Helsinki error publishes alert, increments error counter, and resets active gauge`() {
        whenever(helsinkiSiiloService.laheta(any(), any()))
            .thenThrow(RuntimeException("HY Siilo epäonnistui"))

        assertThrows(RuntimeException::class.java) {
            lahetaService.laheta(
                yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                filePath = "/tmp/fail.zip",
                caseType = CaseType.VALMISTUMINEN,
                yek = false
            )
        }

        verify(alertPublisherService).publishAlert(any(), any())

        assertThat(errorCount(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.VALMISTUMINEN)).isEqualTo(1.0)
        assertThat(successCount(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.VALMISTUMINEN)).isEqualTo(0.0)
        assertThat(arkistointiMetrics.activeArkistointiOperations.get()).isEqualTo(0)
    }

    @Test
    fun `laheta Tampere error publishes alert, increments error counter, and resets active gauge`() {
        whenever(tampereLouhiService.laheta(any(), any()))
            .thenThrow(RuntimeException("SFTP timeout"))

        assertThrows(RuntimeException::class.java) {
            lahetaService.laheta(
                yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
                filePath = "/tmp/tre.zip",
                caseType = CaseType.KOEJAKSO,
                yek = false
            )
        }

        verify(alertPublisherService).publishAlert(any(), any())

        assertThat(errorCount(YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.KOEJAKSO)).isEqualTo(1.0)
        assertThat(successCount(YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.KOEJAKSO)).isEqualTo(0.0)
        assertThat(arkistointiMetrics.activeArkistointiOperations.get()).isEqualTo(0)
    }

    @Test
    fun `laheta universities without integration do not update error counter and do not publish alert`() {
        lahetaService.laheta(
            yliopisto = YliopistoEnum.OULUN_YLIOPISTO,
            filePath = "/tmp/oulu.zip",
            caseType = CaseType.VALMISTUMINEN,
            yek = false
        )

        verify(alertPublisherService, never()).publishAlert(any(), any())
        assertThat(successCount(YliopistoEnum.OULUN_YLIOPISTO, CaseType.VALMISTUMINEN)).isEqualTo(1.0)
        assertThat(errorCount(YliopistoEnum.OULUN_YLIOPISTO, CaseType.VALMISTUMINEN)).isEqualTo(0.0)
    }


    private fun successCount(yliopisto: YliopistoEnum, caseType: CaseType): Double =
        metricsRegistry.find("arkistointi.requests.total")
            .tags("yliopisto", yliopisto.name, "caseType", caseType.value)
            .counter()?.count() ?: 0.0

    private fun errorCount(yliopisto: YliopistoEnum, caseType: CaseType): Double =
        metricsRegistry.find("arkistointi.errors.total")
            .tags("yliopisto", yliopisto.name, "caseType", caseType.value)
            .counter()?.count() ?: 0.0

    private fun createService(zipMetadata: Boolean): ArkistointiServiceImpl {
        val applicationProperties = ApplicationProperties()

        applicationProperties.getArkistointi().getTre().host = "localhost"
        applicationProperties.getArkistointi().getTre().port = "22"
        applicationProperties.getArkistointi().getTre().user = "test-user"
        applicationProperties.getArkistointi().getTre().metadata = createMetadata(zipMetadata)

        return ArkistointiServiceImpl(
            applicationProperties = applicationProperties,
            tampereLouhiService = TampereLouhiService(org.springframework.core.io.DefaultResourceLoader(), applicationProperties),
            helsinkiSiiloService = HelsinkiSiiloService(applicationProperties),
            alertPublisherService = object : AlertPublisherService {
                override fun publishAlert(subject: String, message: String) = Unit
            },
            arkistointiMetrics = ArkistointiMetricsService(SimpleMeterRegistry())
        )
    }

    private fun createMetadata(zipMetadata: Boolean): ApplicationProperties.Arkistointi.Metadata {
        val contact = ApplicationProperties.Arkistointi.Contact().apply {
            person = "Yhteyshenkilo"
            address = "Katu 1"
            phone = "010101"
            email = "test@example.com"
        }

        val documentMetadata = ApplicationProperties.Arkistointi.DocumentMetadata().apply {
            retentionPeriod = "10"
        }

        val case = ApplicationProperties.Arkistointi.Case().apply {
            title = "Valmistumisen asiakirjat"
            type = "VALMISTUMINEN"
            function = "04.01.04"
            documents = mapOf(RecordType.YHTEENVETO.name.lowercase() to documentMetadata)
        }

        return ApplicationProperties.Arkistointi.Metadata().apply {
            this.zipMetadata = zipMetadata
            organisation = "Testiorganisaatio"
            retentionReason = "Lakisateinen syy"
            retentionPeriod = "100"
            useType = "Arkisto"
            this.contact = contact
            cases = mapOf(CaseType.VALMISTUMINEN.value to case)
        }
    }

    private fun createOpintooikeus(): Opintooikeus {
        val user = User(firstName = "Matti", lastName = "Meikalainen")
        val kayttaja = Kayttaja(user = user)
        val erikoistuva = ErikoistuvaLaakari(
            kayttaja = kayttaja,
            syntymaaika = LocalDate.of(1990, 12, 31)
        )

        return Opintooikeus(
            id = 123L,
            erikoistuvaLaakari = erikoistuva,
            yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO),
            erikoisala = Erikoisala(nimi = "Kirurgia"),
            opiskelijatunnus = "OP123"
        )
    }

    private fun createRecordProperties(opintooikeus: Opintooikeus): RecordProperties {
        val asiakirja = Asiakirja(
            id = 321L,
            opintooikeus = opintooikeus,
            nimi = "testi.pdf",
            tyyppi = "application/pdf",
            asiakirjaData = AsiakirjaData(data = "pdf-data".toByteArray())
        )

        return RecordProperties(
            asiakirja = asiakirja,
            type = RecordType.YHTEENVETO
        )
    }

    private fun assertXmlContent(xml: String) {
        assertTrue(xml.startsWith("<?xml"))
        assertTrue(xml.contains("<CaseFile"))
        assertTrue(xml.contains("<Title>Valmistumisen asiakirjat</Title>"))
        assertTrue(xml.contains("<NativeId>CASE-123</NativeId>"))
        assertTrue(xml.contains("<Elsa_Syntymaaika>1990-12-31</Elsa_Syntymaaika>"))
        assertTrue(xml.contains("<Elsa_Tarkastuspaiva>2024-01-02</Elsa_Tarkastuspaiva>"))
        assertTrue(xml.contains("<Elsa_Hyvaksymispaiva>2024-01-03</Elsa_Hyvaksymispaiva>"))
        assertTrue(xml.contains("<Path>pdf/testi.pdf</Path>"))
        assertTrue(xml.contains("<HashValue>${DigestUtils.sha256Hex("pdf-data".toByteArray())}</HashValue>"))
        assertTrue(xml.contains("<Elsa_Yliopisto>Tampereen yliopisto</Elsa_Yliopisto>"))
    }

    private fun readZipEntries(zipFilePath: String): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(File(zipFilePath).inputStream()).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                entries[entry.name] = zipInput.readBytes()
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        return entries
    }
}

