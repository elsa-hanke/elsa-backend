package fi.elsapalvelu.elsa.service.arkistointi.sahke


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestData
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiDispatcher
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiServiceImpl
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID
import java.util.MissingResourceException
import java.util.zip.ZipFile

class AsyncSahkePackageBuilderTest {
    @TempDir lateinit var directory: Path
    private val mapper = jacksonObjectMapper()
    private val properties = ArkistointiTestData.createApplicationProperties()
    private val configuration = ArkistointiConfigurationProvider(properties)
    private val builder = AsyncSahkePackageBuilder(SahkePayloadReader(mapper), configuration, SahkeMetadataBuilder(), SahkePakettiBuilder())

    @ParameterizedTest
    @CsvSource("VALMISTUMINEN,false,false", "VALMISTUMINEN,true,false", "KOEJAKSO,false,false",
        "VALMISTUMINEN,false,true", "VALMISTUMINEN,true,true", "KOEJAKSO,false,true", "SOPIMUS,false,true")
    fun `saved submission produces exactly the legacy XML and PDF entries`(caseType: CaseType, yek: Boolean, helsinki: Boolean) {
        val scenarios = if (helsinki) ArkistointiTestData.helsinkiScenarios else ArkistointiTestData.tamperePackageScenarios
        val scenario = scenarios.single { it.caseType == caseType }
        val input = ArkistointiTestData.createInput(scenario)
        val studyRight = input.opintooikeus
        val user = studyRight.erikoistuvaLaakari!!.kayttaja!!.user!!
        val payload = mapper.writeValueAsString(mapOf(
            "valmistumispyyntoId" to 42, "caseId" to 42, "opintooikeusId" to studyRight.id,
            "university" to scenario.yliopisto.name, "yek" to yek,
            "traineeName" to user.getName(), "firstName" to user.firstName, "lastName" to user.lastName,
            "birthDate" to studyRight.erikoistuvaLaakari!!.syntymaaika.toString(),
            "specialtyName" to studyRight.erikoisala!!.nimi, "studentNumber" to studyRight.opiskelijatunnus,
            "reviewer" to "Reviewer", "reviewDate" to LocalDate.now().minusDays(1).toString(),
            "approver" to "Approver", "approvalDate" to LocalDate.now().toString(),
            "documents" to input.asiakirjat.mapIndexed { index, record ->
                ArkistointiDocumentIdentity(record.asiakirja.id!!, record.asiakirja.id!! + 10000, record.type, index)
            }
        ))
        val documents = input.asiakirjat.mapIndexed { index, record ->
            val document = record.asiakirja
            LoadedArkistointiAsiakirja(document.id!! + 10000, record.type, index, document.nimi!!, document.tyyppi!!,
                DigestUtils.sha256Hex(document.asiakirjaData!!.data), document.asiakirjaData!!.data!!)
        }
        val submission = ArkistointiSubmission(UUID.randomUUID(), scenario.yliopisto, caseType, payload, documents)
        val legacy = SynchronousArkistointiServiceImpl(configuration, SahkeMetadataBuilder(), SahkePakettiBuilder(), mock<SynchronousArkistointiDispatcher>())
            .muodostaSahke(studyRight, input.asiakirjat, "42", "Reviewer", LocalDate.now().minusDays(1),
                "Approver", LocalDate.now(), scenario.yliopisto, caseType)
        try {
            // Changes after approval must not leak into delivery.
            user.firstName = "Changed after approval"
            studyRight.opiskelijatunnus = "Changed after approval"
            val output = buildIn(submission, "async")
            assertThat(output.yek).isEqualTo(yek)
            assertThat(output.path.fileName).isEqualTo(Path.of(legacy.zipFilePath).fileName)
            assertZipContentsEqual(Path.of(legacy.zipFilePath), output.path)
            val second = buildIn(submission, "retry")
            assertZipContentsEqual(output.path, second.path)

            val historicalPayload = (mapper.readTree(payload) as ObjectNode).put("approvalDate", "2024-12-31")
            val historical = buildIn(submission.copy(payload = historicalPayload.toString()), "historical")
            ZipFile(historical.path.toFile()).use { zip ->
                val xml = zip.getInputStream(zip.getEntry("sahke.xml")).bufferedReader().use { it.readText() }
                assertThat(xml).contains("urn:oid:1.2.246.582.200.${studyRight.id}2024.2024.0001")
                assertThat(xml).doesNotContain("Changed after approval")
            }

            assertThatThrownBy {
                builder.build(submission.copy(asiakirjat = listOf(documents.first().copy(filename = "../escape.pdf"))),
                    directory.resolve("unsafe.zip"))
            }.isInstanceOf(IllegalArgumentException::class.java)
            assertThat(directory.resolve("unsafe.zip")).doesNotExist()
        } finally {
            Files.deleteIfExists(Path.of(legacy.zipFilePath))
        }
    }

    private fun buildIn(submission: ArkistointiSubmission, folder: String): PreparedSahkePackage =
        builder.build(submission, Files.createDirectory(directory.resolve(folder)))

    @Test
    fun `university mismatch is rejected before writing files`() {
        val submission = ArkistointiSubmission(UUID.randomUUID(), ArkistointiTestData.tamperePackageScenarios.first().yliopisto,
            CaseType.VALMISTUMINEN, "{\"university\":\"HELSINGIN_YLIOPISTO\"}", emptyList())
        assertThatThrownBy { builder.build(submission, directory.resolve("invalid.zip")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(directory.resolve("invalid.zip")).doesNotExist()
    }

    private fun assertZipContentsEqual(expected: Path, actual: Path) {
        ZipFile(expected.toFile()).use { expectedZip ->
            ZipFile(actual.toFile()).use { actualZip ->
                val names = expectedZip.entries().asSequence().map { it.name }.toList()
                assertThat(actualZip.entries().asSequence().map { it.name }.toList()).containsExactlyElementsOf(names)
                names.forEach { name ->
                    assertThat(actualZip.getInputStream(actualZip.getEntry(name)).use { it.readBytes() })
                        .describedAs(name)
                        .isEqualTo(expectedZip.getInputStream(expectedZip.getEntry(name)).use { it.readBytes() })
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["case", "zip", "document"])
    fun `missing mandatory configuration is classified before ZIP creation`(missing: String) {
        val submission = validSubmission()
        val metadata = properties.getArkistointi().getTre().metadata!!
        when (missing) {
            "case" -> metadata.cases = emptyMap()
            "zip" -> metadata.zipMetadata = false
            "document" -> metadata.getCaseMetadata(submission.caseType)!!.documents = emptyMap()
        }
        assertThatThrownBy { builder.build(submission, directory) }
            .isInstanceOfSatisfying(SahkePackageException::class.java) {
                assertThat(it.failure).isEqualTo(SahkePackageFailure.CONFIGURATION_INVALID)
                assertThat(it.cause).isNull()
            }
        Files.list(directory).use { assertThat(it.count()).isZero() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["{", "null", "[]", "date", "identifier", "filename"])
    fun `known invalid payload is classified before ZIP creation`(invalid: String) {
        val valid = validSubmission()
        val payload = mapper.readTree(valid.payload) as ObjectNode
        val submission = when (invalid) {
            "date" -> valid.copy(payload = payload.put("approvalDate", "private-invalid-date").toString())
            "identifier" -> valid.copy(payload = payload.put("opintooikeusId", -1).toString())
            "filename" -> valid.copy(asiakirjat = listOf(valid.asiakirjat.first().copy(filename = "../private.pdf")))
            else -> valid.copy(payload = invalid)
        }
        assertThatThrownBy { builder.build(submission, directory) }
            .isInstanceOfSatisfying(SahkePackageException::class.java) {
                assertThat(it.failure).isEqualTo(SahkePackageFailure.PAYLOAD_INVALID)
                assertThat(it.message).doesNotContain("private")
                assertThat(it.cause).isNull()
            }
        Files.list(directory).use { assertThat(it.count()).isZero() }
    }

    @Test
    fun `missing localization is a safe permanent failure without creating ZIP`() {
        val metadata = mock<SahkeMetadataBuilder>()
        val packages = SahkePakettiBuilder()
        whenever(metadata.build(any(), any()))
            .thenThrow(MissingResourceException("private resource details", "private", "private"))
        val subject = AsyncSahkePackageBuilder(SahkePayloadReader(mapper), configuration, metadata, packages)

        assertThatThrownBy { subject.build(validSubmission(), directory) }
            .isInstanceOfSatisfying(SahkePackageException::class.java) {
                assertThat(it.failure).isEqualTo(SahkePackageFailure.LOCALIZATION_MISSING)
                assertThat(it.message).doesNotContain("private")
                assertThat(it.cause).isNull()
            }
        Files.list(directory).use { assertThat(it.count()).isZero() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing", "empty", "duplicate", "extra", "snapshot", "type", "order", "source", "malformed", "duplicate-loaded"])
    fun `invalid document identity manifest is rejected before writing ZIP`(invalid: String) {
        val valid = validSubmission()
        val payload = mapper.readTree(valid.payload) as ObjectNode
        val identities = payload.withArray("documents")
        val first = identities[0] as ObjectNode
        val submission = when (invalid) {
            "missing" -> { payload.remove("documents"); valid }
            "empty" -> { identities.removeAll(); valid }
            "duplicate" -> { identities.set(1, first.deepCopy()); valid }
            "extra" -> { identities.add(first.deepCopy()); valid }
            "snapshot" -> { first.put("snapshotDocumentId", 999999); valid }
            "type" -> { first.put("recordType", "SOPIMUS"); valid }
            "order" -> { first.put("jarjestysnumero", 999); valid }
            "source" -> { first.put("sourceDocumentId", 0); valid }
            "malformed" -> { first.put("recordType", "private-invalid-type"); valid }
            else -> valid.copy(asiakirjat = listOf(valid.asiakirjat.first(), valid.asiakirjat.first()))
        }.copy(payload = payload.toString())

        assertThatThrownBy { builder.build(submission, directory) }
            .isInstanceOfSatisfying(SahkePackageException::class.java) {
                assertThat(it.failure).isEqualTo(SahkePackageFailure.PAYLOAD_INVALID)
                assertThat(it.message).doesNotContain("private")
                assertThat(it.cause).isNull()
            }
        Files.list(directory).use { assertThat(it.count()).isZero() }
    }

    private fun validSubmission(): ArkistointiSubmission {
        val scenario = ArkistointiTestData.tamperePackageScenarios.first()
        val input = ArkistointiTestData.createInput(scenario)
        val documents = input.asiakirjat.mapIndexed { index, record ->
            val document = record.asiakirja
            LoadedArkistointiAsiakirja(document.id!!, record.type, index, document.nimi!!, document.tyyppi!!,
                DigestUtils.sha256Hex(document.asiakirjaData!!.data), document.asiakirjaData!!.data!!)
        }
        val payload = mapper.writeValueAsString(mapOf(
            "university" to scenario.yliopisto.name, "yek" to false,
            "approvalDate" to "2026-01-01", "opintooikeusId" to 1,
            "valmistumispyyntoId" to 42, "caseId" to 42,
            "documents" to documents.map {
                ArkistointiDocumentIdentity(it.asiakirjaId + 10000, it.asiakirjaId, it.asiakirjatyyppi, it.jarjestysnumero)
            }
        ))
        return ArkistointiSubmission(UUID.randomUUID(), scenario.yliopisto, scenario.caseType, payload, documents)
    }
}
