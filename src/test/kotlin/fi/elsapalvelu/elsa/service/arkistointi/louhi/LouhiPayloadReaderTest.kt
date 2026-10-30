package fi.elsapalvelu.elsa.service.arkistointi.louhi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

class LouhiPayloadReaderTest {
    private val reader = LouhiPayloadReader(jacksonObjectMapper())

    @ParameterizedTest
    @EnumSource(CaseType::class)
    fun `reads approval values and chooses the operation identifier`(caseType: CaseType) {
        val payload = reader.read(PAYLOAD, caseType)

        assertThat(payload.caseId).isEqualTo(if (caseType == CaseType.VALMISTUMINEN) 42L else 43L)
        assertThat(payload.university).isEqualTo("TAMPEREEN_YLIOPISTO")
        assertThat(payload.studyRightId).isEqualTo(11L)
        assertThat(payload.yek).isFalse()
        assertThat(payload.approvalDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(payload.reviewDate).isEqualTo(LocalDate.of(2024, 12, 29))
        assertThat(payload.birthDate).isEqualTo(LocalDate.of(1991, 2, 3))
        assertThat(payload.traineeName).isEqualTo("Private trainee")
        assertThat(payload.firstName).isEqualTo("Private")
        assertThat(payload.lastName).isEqualTo("trainee")
        assertThat(payload.specialtyName).isEqualTo("Private specialty")
        assertThat(payload.studentNumber).isEqualTo("Private student number")
        assertThat(payload.reviewer).isEqualTo("Private reviewer")
        assertThat(payload.approver).isEqualTo("Private approver")
        assertThat(payload.documents.single().sourceDocumentId).isEqualTo(51L)
        assertThat(payload.documents.single().snapshotDocumentId).isEqualTo(61L)
        assertThat(payload.documents.single().recordType).isEqualTo(RecordType.YHTEENVETO)
        assertThat(payload.documents.single().jarjestysnumero).isZero()
        assertThat(payload.toString()).isEqualTo("LouhiArchivePayload")
    }

    @Test
    fun `optional missing and null fields stay optional`() {
        val payload = reader.read(MINIMAL_PAYLOAD, CaseType.VALMISTUMINEN)

        assertThat(payload.traineeName).isNull()
        assertThat(payload.firstName).isNull()
        assertThat(payload.lastName).isNull()
        assertThat(payload.birthDate).isNull()
        assertThat(payload.specialtyName).isNull()
        assertThat(payload.studentNumber).isNull()
        assertThat(payload.reviewer).isNull()
        assertThat(payload.reviewDate).isNull()
        assertThat(payload.approver).isNull()
    }

    @ParameterizedTest
    @MethodSource("invalidPayloads")
    fun `rejects malformed values without coercion or exposing payload`(json: String) {
        assertThatThrownBy { reader.read(json, CaseType.VALMISTUMINEN) }
            .isInstanceOfSatisfying(LouhiPackageException::class.java) {
                assertThat(it.failure).isEqualTo(LouhiPackageFailure.PAYLOAD_INVALID)
                assertThat(it.message).isEqualTo("LOUHI_PAYLOAD_INVALID")
                assertThat(it.cause).isNull()
                assertThat(it.suppressed).isEmpty()
            }
    }

    companion object {
        private const val MINIMAL_PAYLOAD = """{
            "university": "TAMPEREEN_YLIOPISTO", "yek": false,
            "opintooikeusId": 11, "valmistumispyyntoId": 42, "caseId": 43,
            "approvalDate": "2024-12-31", "firstName": null,
            "documents": [{"sourceDocumentId": 51, "snapshotDocumentId": 61,
                "recordType": "YHTEENVETO", "jarjestysnumero": 0}]
        }"""
        private val PAYLOAD = MINIMAL_PAYLOAD.replace("\"firstName\": null", """
            "firstName": "Private", "lastName": "trainee", "traineeName": "Private trainee",
            "birthDate": "1991-02-03", "specialtyName": "Private specialty",
            "studentNumber": "Private student number", "reviewer": "Private reviewer",
            "reviewDate": "2024-12-29", "approver": "Private approver"
        """)

        @JvmStatic
        fun invalidPayloads(): Stream<String> = Stream.of(
            "", "{", "null", "[]",
            PAYLOAD.replace("\"opintooikeusId\": 11", "\"opintooikeusId\": \"11\""),
            PAYLOAD.replace("\"opintooikeusId\": 11", "\"opintooikeusId\": 1.5"),
            PAYLOAD.replace("\"opintooikeusId\": 11", "\"opintooikeusId\": 9223372036854775808"),
            PAYLOAD.replace("\"opintooikeusId\": 11", "\"opintooikeusId\": null"),
            PAYLOAD.replace("\"opintooikeusId\": 11", "\"opintooikeusId\": 0"),
            PAYLOAD.replace("\"valmistumispyyntoId\": 42", "\"valmistumispyyntoId\": false"),
            PAYLOAD.replace("\"yek\": false", "\"yek\": \"false\""),
            PAYLOAD.replace("\"yek\": false", "\"yek\": 0"),
            PAYLOAD.replace("\"approvalDate\": \"2024-12-31\"", "\"approvalDate\": null"),
            PAYLOAD.replace("2024-12-31", "Private invalid date"),
            PAYLOAD.replace("\"firstName\": \"Private\"", "\"firstName\": 123"),
            PAYLOAD.replace("\"documents\": [", "\"missingDocuments\": ["),
            PAYLOAD.replace("YHTEENVETO", "Private invalid record type")
        )
    }
}
