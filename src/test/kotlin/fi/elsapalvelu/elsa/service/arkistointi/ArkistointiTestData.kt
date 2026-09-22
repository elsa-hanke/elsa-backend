package fi.elsapalvelu.elsa.service.arkistointi

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
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

data class ArkistointiTestScenario(
    val name: String,
    val yliopisto: YliopistoEnum,
    val caseType: CaseType,
    val recordTypes: List<RecordType>,
    val yek: Boolean = false
) {
    override fun toString(): String = name
}

data class ArkistointiTestInput(
    val scenario: ArkistointiTestScenario,
    val correlationId: String,
    val opintooikeus: Opintooikeus,
    val asiakirjat: List<RecordProperties>,
    val caseId: String
)

object ArkistointiTestData {

    val helsinkiScenarios = listOf(
        ArkistointiTestScenario(
            name = "Helsinki valmistuminen collection 15",
            yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN,
            recordTypes = listOf(RecordType.YHTEENVETO)
        ),
        ArkistointiTestScenario(
            name = "Helsinki sopimus collection 16",
            yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
            caseType = CaseType.SOPIMUS,
            recordTypes = listOf(RecordType.SOPIMUS)
        ),
        ArkistointiTestScenario(
            name = "Helsinki koejakso collection 17",
            yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
            caseType = CaseType.KOEJAKSO,
            recordTypes = listOf(RecordType.ARVIOINTI)
        )
    )

    val tamperePackageScenarios = listOf(
        ArkistointiTestScenario(
            name = "Tampere valmistuminen package",
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN,
            recordTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE)
        ),
        ArkistointiTestScenario(
            name = "Tampere koejakso package",
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.KOEJAKSO,
            recordTypes = listOf(RecordType.ARVIOINTI)
        )
    )

    val tampereDeliveryScenarios = listOf(
        ArkistointiTestScenario(
            name = "Tampere Finished ELSA destination",
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN,
            recordTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE),
            yek = false
        ),
        ArkistointiTestScenario(
            name = "Tampere Finished YEK destination",
            yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN,
            recordTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE),
            yek = true
        )
    )

    fun createInput(
        scenario: ArkistointiTestScenario,
        correlationId: String = createCorrelationId()
    ): ArkistointiTestInput {
        val opintooikeus = createOpintooikeus(scenario.yliopisto, correlationId)
        val pdfBytes = createTestPdf(correlationId)
        val asiakirjat = scenario.recordTypes.mapIndexed { index, recordType ->
            val documentId = uniqueLong("$correlationId-document-$index")
            val name = "${recordType.name.lowercase()}_elsa_integration_$correlationId.pdf"
            RecordProperties(
                Asiakirja(
                    id = documentId,
                    opintooikeus = opintooikeus,
                    nimi = name,
                    tyyppi = "application/pdf",
                    asiakirjaData = AsiakirjaData(id = documentId, data = pdfBytes)
                ),
                recordType
            )
        }

        return ArkistointiTestInput(
            scenario = scenario,
            correlationId = correlationId,
            opintooikeus = opintooikeus,
            asiakirjat = asiakirjat,
            caseId = "ELSA-INTEGRATION-${scenario.caseType.value}-$correlationId"
        )
    }

    fun createPackage(
        arkistointiService: ArkistointiServiceImpl,
        input: ArkistointiTestInput
    ): ArkistointiResult = arkistointiService.muodostaSahke(
        opintooikeus = input.opintooikeus,
        asiakirjat = input.asiakirjat,
        caseId = input.caseId,
        tarkastaja = "ELSA integraatiotesti",
        tarkastusPaiva = LocalDate.now().minusDays(2),
        hyvaksyja = "ELSA integraatiotesti",
        hyvaksymisPaiva = LocalDate.now().minusDays(1),
        yliopisto = input.scenario.yliopisto,
        caseType = input.scenario.caseType
    )

    fun createApplicationProperties(): ApplicationProperties = ApplicationProperties().apply {
        getArkistointi().getHki().apply {
            kaytossa = true
            metadata = createHelsinkiMetadata()
        }
        getArkistointi().getTre().apply {
            kaytossa = true
            metadata = createTampereMetadata()
        }
    }

    private fun createTestPdf(correlationId: String): ByteArray {
        val paragraph =
            "ELSA integraatiotestin tuottama testiaineisto arkistointia varten. " +
                "Correlation-id: $correlationId. Tama sisalto on tarkoituksella pidempi, " +
                "jotta lahetettava paketti vastaa kooltaan oikeaa arkistoitavaa asiakirjaa " +
                "eika vastaanottava jarjestelma tulkitse pyyntoa virheellisesti liian " +
                "pieneksi tai puutteelliseksi. "

        return renderPdf(paragraph, TEST_PDF_PAGE_COUNT)
    }

    private fun renderPdf(paragraph: String, pageCount: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            PDDocument().use { document ->
                val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
                repeat(pageCount) { pageIndex ->
                    val page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    PDPageContentStream(document, page).use { stream ->
                        stream.beginText()
                        stream.setFont(font, 11f)
                        stream.setLeading(14f)
                        stream.newLineAtOffset(50f, 780f)
                        stream.showText("Sivu ${pageIndex + 1}")
                        stream.newLine()
                        wrapText(paragraph, WRAP_LINE_LENGTH).forEach { line ->
                            stream.showText(line)
                            stream.newLine()
                        }
                        stream.endText()
                    }
                }
                document.save(output)
            }
            output.toByteArray()
        }

    private fun wrapText(text: String, lineLength: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        words.forEach { word ->
            if (currentLine.length + word.length + 1 > lineLength) {
                lines.add(currentLine.toString())
                currentLine.clear()
            }
            if (currentLine.isNotEmpty()) currentLine.append(' ')
            currentLine.append(word)
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun createOpintooikeus(
        yliopisto: YliopistoEnum,
        correlationId: String
    ): Opintooikeus {
        val opintooikeusId = uniqueLong("$correlationId-study-right")
        val user = User(
            firstName = "ELSA",
            lastName = "Integration-$correlationId"
        )
        return Opintooikeus(
            id = opintooikeusId,
            opiskelijatunnus = "TEST-$correlationId",
            erikoistuvaLaakari = ErikoistuvaLaakari(
                id = uniqueLong("$correlationId-student"),
                syntymaaika = LocalDate.of(1990, 1, 1),
                kayttaja = Kayttaja(user = user)
            ),
            yliopisto = Yliopisto(id = uniqueLong("$correlationId-university"), nimi = yliopisto),
            erikoisala = Erikoisala(
                id = uniqueLong("$correlationId-speciality"),
                nimi = "ELSA integraatiotesti"
            )
        )
    }

    private fun createHelsinkiMetadata() = ApplicationProperties.Arkistointi.Metadata().apply {
        zipMetadata = true
        organisation = "Helsingin yliopisto"
        retentionReason = "Helsingin yliopiston päätös (tiedonhallintamalli)"
        retentionPeriod = "20"
        useType = "Säilytys"
        cases = mapOf(
            CaseType.VALMISTUMINEN.value to createCase(
                "Valmistumisen yhteenveto",
                "Raportti",
                "04.12",
                RecordType.YHTEENVETO
            ),
            CaseType.SOPIMUS.value to createCase(
                "Koulutuspaikkasopimus",
                "Sopimus",
                "04.12",
                RecordType.SOPIMUS
            ),
            CaseType.KOEJAKSO.value to createCase(
                "Koejakson arviointi",
                "Muu",
                "04.12",
                RecordType.ARVIOINTI
            )
        )
    }

    private fun createTampereMetadata() = ApplicationProperties.Arkistointi.Metadata().apply {
        zipMetadata = true
        organisation = "TAU"
        retentionReason = "Sisältää henkilötietoja"
        retentionPeriod = "-1"
        cases = mapOf(
            CaseType.VALMISTUMINEN.value to createCase(
                "Valmistuminen",
                "Valmistuminen",
                "04.02.05",
                RecordType.YHTEENVETO,
                RecordType.LIITE
            ),
            CaseType.KOEJAKSO.value to createCase(
                "Koejakson arviointi",
                "Koejakson arviointi",
                "04.01.04",
                RecordType.ARVIOINTI
            )
        )
    }

    private fun createCase(
        title: String,
        type: String,
        function: String,
        vararg recordTypes: RecordType
    ) = ApplicationProperties.Arkistointi.Case().apply {
        this.title = title
        this.type = type
        this.function = function
        documents = recordTypes.associate { recordType ->
            recordType.name.lowercase() to ApplicationProperties.Arkistointi.DocumentMetadata().apply {
                retentionPeriod = "20"
            }
        }
    }

    private fun createCorrelationId(): String {
        val buildId = (System.getenv("CODEBUILD_BUILD_ID") ?: "local")
            .replace(NON_IDENTIFIER_CHARACTER, "-")
        val uniqueSuffix = UUID.randomUUID().toString().take(UUID_SUFFIX_LENGTH)
        val buildIdMaxLength = MAX_CORRELATION_ID_LENGTH - UUID_SUFFIX_LENGTH - 1
        return "${buildId.take(buildIdMaxLength)}-$uniqueSuffix"
    }

    private fun uniqueLong(value: String): Long {
        val uuid = UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8))
        return (uuid.mostSignificantBits and Long.MAX_VALUE).coerceAtLeast(1L)
    }

    private val NON_IDENTIFIER_CHARACTER = Regex("[^A-Za-z0-9_-]")
    private const val UUID_SUFFIX_LENGTH = 8
    private const val MAX_CORRELATION_ID_LENGTH = 96
    private const val WRAP_LINE_LENGTH = 90
    private const val TEST_PDF_PAGE_COUNT = 200
}

