package fi.elsapalvelu.elsa.service.arkistointi.job.payload



import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.stream.Stream

class ArchivePayloadReaderTest {
    private val reader = ArchivePayloadReader(jacksonObjectMapper())

    @Test
    fun `reads approval values and the common case identifier`() {
        val payload = reader.read(PAYLOAD)

        assertThat(payload.caseId).isEqualTo(42L)
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
        assertThat(payload.toString()).isEqualTo("ArchivePayload")
    }

    @Test
    fun `optional missing and null fields stay optional`() {
        val payload = reader.read(MINIMAL_PAYLOAD)

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
        assertThatThrownBy { reader.read(json) }
            .isInstanceOfSatisfying(ArchivePayloadException::class.java) {
                assertThat(it.message).isEqualTo("ARCHIVE_PAYLOAD_INVALID")
                assertThat(it.cause).isNull()
                assertThat(it.suppressed).isEmpty()
            }
    }

    @Test
    fun `reads optional typed Dynasty fields`() {
        val root = jacksonObjectMapper().readTree(MINIMAL_PAYLOAD) as com.fasterxml.jackson.databind.node.ObjectNode
        root.put("specialtyType", "HAMMASLAAKETIEDE")
        root.put("studyRightStartDate", "2020-01-01")
        root.put("degreeRegulation", "56/2015")
        root.put("licensingDate", "2019-01-01")
        val payload = reader.read(root.toString())
        assertThat(payload.specialtyType).isEqualTo("HAMMASLAAKETIEDE")
        assertThat(payload.studyRightStartDate).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(payload.degreeRegulation).isEqualTo("56/2015")
        assertThat(payload.licensingDate).isEqualTo(LocalDate.of(2019, 1, 1))
        val minimalPayload = reader.read(MINIMAL_PAYLOAD)
        assertThat(minimalPayload.specialtyType).isNull()
        assertThat(minimalPayload.studyRightStartDate).isNull()
        assertThat(minimalPayload.degreeRegulation).isNull()
        assertThat(minimalPayload.licensingDate).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["specialtyType", "studyRightStartDate", "degreeRegulation", "licensingDate"])
    fun `rejects nontextual Dynasty fields without scalar coercion`(field: String) {
        val root = jacksonObjectMapper().readTree(MINIMAL_PAYLOAD) as com.fasterxml.jackson.databind.node.ObjectNode
        root.put(field, 123)
        assertThatThrownBy { reader.read(root.toString()) }
            .isInstanceOf(ArchivePayloadException::class.java).hasMessage("ARCHIVE_PAYLOAD_INVALID").hasNoCause()
    }

    @ParameterizedTest
    @ValueSource(strings = ["studyRightStartDate", "licensingDate"])
    fun `rejects malformed Dynasty dates without retaining private values`(field: String) {
        val root = jacksonObjectMapper().readTree(MINIMAL_PAYLOAD) as com.fasterxml.jackson.databind.node.ObjectNode
        root.put(field, "Private invalid date")
        assertThatThrownBy { reader.read(root.toString()) }
            .isInstanceOf(ArchivePayloadException::class.java).hasMessage("ARCHIVE_PAYLOAD_INVALID").hasNoCause()
    }

    companion object {
        private const val MINIMAL_PAYLOAD = """{
            "university": "TAMPEREEN_YLIOPISTO", "yek": false,
            "version": 1, "opintooikeusId": 11, "caseId": 42,
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
            PAYLOAD.replace("\"caseId\": 42", "\"caseId\": false"),
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
