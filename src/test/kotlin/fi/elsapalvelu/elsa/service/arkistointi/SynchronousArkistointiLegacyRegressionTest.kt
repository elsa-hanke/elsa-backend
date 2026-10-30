package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.mock
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.stream.Stream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList

/**
 * Expected XML was captured from commit c76d6d293c50bec7880be8e6eb998c2c1002aa4f,
 * before SahkePerson. See resources/fixtures/arkistointi/README.md.
 */
class SynchronousArkistointiLegacyRegressionTest {
    @ParameterizedTest(name = "{0}, XML in ZIP: {1}")
    @MethodSource("scenarios")
    fun `sync package preserves pre-async XML and document bytes`(
        scenario: ArkistointiTestScenario,
        zipMetadata: Boolean
    ) {
        val properties = ArkistointiTestData.createApplicationProperties()
        properties.getArkistointi().getHki().metadata!!.zipMetadata = zipMetadata
        properties.getArkistointi().getTre().metadata!!.zipMetadata = zipMetadata
        val configuration = ArkistointiConfigurationProvider(properties)
        val studyRight = studyRight(scenario)
        val pdf = resource("fixtures/valid.pdf")
        val documents = scenario.recordTypes.mapIndexed { index, type ->
            RecordProperties(
                Asiakirja(
                    id = 5101L + index, opintooikeus = studyRight,
                    nimi = "${type.name.lowercase()}.pdf", tyyppi = "application/pdf",
                    asiakirjaData = AsiakirjaData(data = pdf)
                ),
                type
            )
        }
        val today = LocalDate.of(2024, 12, 31)
        val reviewDate = LocalDate.of(2024, 12, 29)
        val approvalDate = LocalDate.of(2024, 12, 30)
        val service = SynchronousArkistointiServiceImpl(
            configuration, SahkeMetadataBuilder(), SahkePakettiBuilder(), mock()
        )

        // Freeze the legacy LocalDate.now() boundary, including the default request date.
        val result = Mockito.mockStatic(LocalDate::class.java, Mockito.CALLS_REAL_METHODS).use { dates ->
            dates.`when`<LocalDate> { LocalDate.now() }.thenReturn(today)
            service.muodostaSahke(
                studyRight, documents, "CASE-4242", "Tarkastaja Å", reviewDate,
                "Hyväksyjä Ö", approvalDate, scenario.yliopisto, scenario.caseType
            )
        }
        val path = Path.of(result.zipFilePath)
        try {
            val expectedName = configuration.getConfiguration(scenario.yliopisto, scenario.caseType)
                .case.type!!.lowercase() + "_ääni_östergård.zip"
            assertThat(path.fileName.toString()).isEqualTo(expectedName)
            ZipFile(path.toFile()).use { zip ->
                val documentEntries = documents.map { "pdf/${it.asiakirja.nimi}" }
                val expectedEntries = if (zipMetadata) listOf("sahke.xml") + documentEntries else documentEntries
                assertThat(zip.entries().asSequence().map { it.name }.toList())
                    .containsExactlyElementsOf(expectedEntries)
                documentEntries.forEach { entry ->
                    assertThat(zip.getInputStream(zip.getEntry(entry)).use { it.readBytes() })
                        .describedAs(entry).isEqualTo(pdf)
                }
                val xml = if (zipMetadata) {
                    assertThat(result.metadataBytes).isNull()
                    zip.getInputStream(zip.getEntry("sahke.xml")).use { it.readBytes() }
                } else {
                    requireNotNull(result.metadataBytes)
                }
                val fixture = "${scenario.yliopisto.name.lowercase()}-${scenario.caseType.name.lowercase()}.xml"
                assertThat(parseXml(xml).documentElement.isEqualNode(parseXml(resource("fixtures/arkistointi/$fixture")).documentElement))
                    .describedAs("XML must match legacy fixture %s", fixture).isTrue()
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private fun parseXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = bytes.inputStream().use { factory.newDocumentBuilder().parse(it) }
        // Ignore indentation between elements, preserving text inside leaf elements verbatim.
        val indentation = XPathFactory.newInstance().newXPath()
            .evaluate("//text()[normalize-space(.) = '' and ../*]", document, XPathConstants.NODESET) as NodeList
        (0 until indentation.length).map { indentation.item(it) }.forEach { it.parentNode.removeChild(it) }
        return document
    }

    private fun studyRight(scenario: ArkistointiTestScenario) = Opintooikeus(
        id = 4101L,
        opiskelijatunnus = "TEST-20240042",
        erikoistuvaLaakari = ErikoistuvaLaakari(
            syntymaaika = LocalDate.of(1991, 2, 3),
            kayttaja = Kayttaja(user = User(firstName = "Ääni", lastName = "Östergård"))
        ),
        erikoisala = Erikoisala(nimi = "Yleislääketiede"),
        yliopisto = Yliopisto(nimi = scenario.yliopisto)
    )

    private fun resource(name: String): ByteArray =
        requireNotNull(javaClass.classLoader.getResourceAsStream(name)) { "Missing fixture: $name" }
            .use { it.readBytes() }

    companion object {
        @JvmStatic
        fun scenarios(): Stream<Array<Any>> =
            (ArkistointiTestData.helsinkiScenarios + ArkistointiTestData.tamperePackageScenarios)
                .flatMap { scenario -> listOf(true, false).map { arrayOf<Any>(scenario, it) } }
                .stream()
    }
}
