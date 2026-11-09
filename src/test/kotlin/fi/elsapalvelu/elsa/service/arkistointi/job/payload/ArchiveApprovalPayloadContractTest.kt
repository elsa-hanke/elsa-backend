package fi.elsapalvelu.elsa.service.arkistointi.job.payload

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Asetus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.koejakso.KoejaksoArkistointiPayload
import fi.elsapalvelu.elsa.service.arkistointi.valmistuminen.ValmistuminenArkistointiPayload
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class ArchiveApprovalPayloadContractTest {
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val reader = ArchivePayloadReader(mapper)
    private val date = LocalDate.of(2026, 11, 4)
    private val documents = listOf(ArkistointiDocumentIdentity(51, 61, RecordType.YHTEENVETO, 0))

    @ParameterizedTest
    @EnumSource(CaseType::class)
    fun `real approval factories serialize every reader field using the versioned contract`(caseType: CaseType) {
        val json = mapper.writeValueAsString(approval(caseType))
        val root = mapper.readTree(json)
        assertThat(root["version"].intValue()).isEqualTo(1)
        assertThat(root["caseId"].longValue()).isEqualTo(42)
        assertThat(root.has("studyRight")).isFalse()
        assertThat(root["university"].textValue()).isEqualTo("TURUN_YLIOPISTO")
        assertThat(root["specialtyType"].textValue()).isEqualTo("HAMMASLAAKETIEDE")
        assertThat(root["specialtyId"].longValue()).isEqualTo(3)
        assertThat(reader.read(json)).isEqualTo(ArchivePayload(
            university = "TURUN_YLIOPISTO", studyRightId = 11, caseId = 42, yek = false,
            approvalDate = date, reviewer = "Private Reviewer", reviewDate = date.minusDays(1), approver = "Private Approver",
            traineeName = "Private Trainee", firstName = "Private", lastName = "Trainee",
            birthDate = date.minusYears(30), specialtyName = "Private specialty", studentNumber = "Private student number",
            documents = documents, specialtyType = "HAMMASLAAKETIEDE", studyRightStartDate = date.minusYears(5),
            degreeRegulation = "56/2015", licensingDate = if (caseType == CaseType.VALMISTUMINEN) date.minusYears(6) else null
        ))
        assertThat(root.has("valmistumispyyntoId")).isFalse()
        if (caseType != CaseType.VALMISTUMINEN) {
            assertThat(root["koejaksoHyvaksytty"].booleanValue()).isFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(CaseType::class)
    fun `version and caseId are mandatory for every approval type`(caseType: CaseType) {
        listOf("version", "caseId").forEach { field ->
            val root = mapper.readTree(mapper.writeValueAsString(approval(caseType))) as ObjectNode
            root.remove(field)
            assertInvalid(root)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["null", "\"1\"", "1.0", "true", "0", "-1", "2", "4294967297", "{}", "[]"])
    fun `unsupported or malformed versions are rejected`(version: String) {
        val root = mapper.readTree(mapper.writeValueAsString(approval(CaseType.VALMISTUMINEN))) as ObjectNode
        root.set<com.fasterxml.jackson.databind.JsonNode>("version", mapper.readTree(version))
        assertInvalid(root)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "university", "firstName", "lastName", "traineeName", "specialtyName", "studentNumber",
        "specialtyType", "reviewer", "approver", "approvalDate", "reviewDate", "birthDate",
        "studyRightStartDate", "degreeRegulation", "licensingDate", "yek"
    ])
    fun `version one retains strict scalar validation`(field: String) {
        val root = mapper.readTree(mapper.writeValueAsString(approval(CaseType.VALMISTUMINEN))) as ObjectNode
        root.put(field, 123)
        assertInvalid(root)
    }

    private fun assertInvalid(root: ObjectNode) {
        assertThatThrownBy { reader.read(root.toString()) }
            .isInstanceOf(ArchivePayloadException::class.java)
            .hasMessage("ARCHIVE_PAYLOAD_INVALID").hasNoCause()
    }

    private fun approval(caseType: CaseType): ArchiveApprovalPayload {
        val university = YliopistoEnum.TURUN_YLIOPISTO
        val studyRight = Opintooikeus(
            id = 11, yliopisto = Yliopisto(nimi = university),
            erikoisala = Erikoisala(id = 3, nimi = "Private specialty", tyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE),
            opiskelijatunnus = "Private student number", opintooikeudenMyontamispaiva = date.minusYears(5),
            asetus = Asetus(nimi = "56/2015"),
            erikoistuvaLaakari = ErikoistuvaLaakari(
                kayttaja = Kayttaja(user = User(firstName = "Private", lastName = "Trainee")),
                syntymaaika = date.minusYears(30), laillistamispaiva = date.minusYears(6)
            )
        )
        return if (caseType == CaseType.VALMISTUMINEN) {
            ValmistuminenArkistointiPayload.create(Valmistumispyynto(
                id = 42, opintooikeus = studyRight,
                virkailija = Kayttaja(user = User(firstName = "Private", lastName = "Reviewer")), virkailijanKuittausaika = date.minusDays(1),
                vastuuhenkiloHyvaksyja = Kayttaja(user = User(firstName = "Private", lastName = "Approver")), vastuuhenkiloHyvaksyjaKuittausaika = date
            ), university, documents)
        } else {
            KoejaksoArkistointiPayload.create(42, studyRight, documents, "Private Reviewer", date.minusDays(1), "Private Approver", date, false)
        }
    }
}
