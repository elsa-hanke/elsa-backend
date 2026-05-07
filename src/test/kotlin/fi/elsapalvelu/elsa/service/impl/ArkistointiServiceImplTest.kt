package fi.elsapalvelu.elsa.service.impl

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
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import java.util.zip.ZipInputStream

class ArkistointiServiceImplTest {

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

    private fun assertXmlContent(xml: String) {
        assertTrue(xml.startsWith("<?xml"))
        assertTrue(xml.contains("<CaseFile>"))
        assertTrue(xml.contains("<Title>Valmistumisen asiakirjat</Title>"))
        assertTrue(xml.contains("<NativeId>CASE-123</NativeId>"))
        assertTrue(xml.contains("<Elsa_Syntymaaika>31.12.1990</Elsa_Syntymaaika>"))
        assertTrue(xml.contains("<Elsa_Tarkastuspaiva>02.01.2024</Elsa_Tarkastuspaiva>"))
        assertTrue(xml.contains("<Elsa_Hyvaksymispaiva>03.01.2024</Elsa_Hyvaksymispaiva>"))
        assertTrue(xml.contains("<Path>pdf/testi.pdf</Path>"))
        assertTrue(xml.contains("<HashValue>${DigestUtils.sha256Hex("pdf-data".toByteArray())}</HashValue>"))
        assertTrue(xml.contains("<Elsa_Yliopisto>Tampereen yliopisto</Elsa_Yliopisto>"))
    }

    private fun createService(zipMetadata: Boolean): ArkistointiServiceImpl {
        val applicationProperties = ApplicationProperties()

        applicationProperties.getArkistointi().getTre().host = "localhost"
        applicationProperties.getArkistointi().getTre().port = "22"
        applicationProperties.getArkistointi().getTre().user = "test-user"
        applicationProperties.getArkistointi().getTre().metadata = createMetadata(zipMetadata)

        return ArkistointiServiceImpl(
            applicationProperties = applicationProperties,
            tampereLouhiService = TampereLouhiService(org.springframework.core.io.DefaultResourceLoader(), applicationProperties),
            helsinkiSiiloService = HelsinkiSiiloService(applicationProperties)
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

