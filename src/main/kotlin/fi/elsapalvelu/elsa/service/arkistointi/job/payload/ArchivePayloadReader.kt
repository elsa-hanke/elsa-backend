package fi.elsapalvelu.elsa.service.arkistointi.job.payload

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Explicit JSON type checks preserve the saved payload contract without scalar coercion. */
@Component
class ArchivePayloadReader(private val mapper: ObjectMapper) {
    fun read(json: String): ArchivePayload {
        val root = try {
            requirePayloadValue(mapper.readTree(json))
        } catch (_: JsonProcessingException) {
            throw ArchivePayloadException()
        }
        requireValidPayload(root.isObject)
        val version = root.path(ArchivePayloadFields.VERSION)
        requireValidPayload(version.isIntegralNumber && version.canConvertToInt() && version.intValue() == ArchivePayloadFields.CURRENT_VERSION)
        val yek = root.path(ArchivePayloadFields.YEK)
        requireValidPayload(yek.isBoolean)
        return ArchivePayload(
            university = requirePayloadValue(text(root, ArchivePayloadFields.UNIVERSITY)),
            studyRightId = identifier(root, ArchivePayloadFields.STUDY_RIGHT_ID),
            caseId = identifier(root, ArchivePayloadFields.CASE_ID),
            yek = yek.booleanValue(),
            approvalDate = requirePayloadValue(date(root, ArchivePayloadFields.APPROVAL_DATE)),
            reviewer = text(root, ArchivePayloadFields.REVIEWER),
            reviewDate = date(root, ArchivePayloadFields.REVIEW_DATE),
            approver = text(root, ArchivePayloadFields.APPROVER),
            traineeName = text(root, ArchivePayloadFields.TRAINEE_NAME),
            firstName = text(root, ArchivePayloadFields.FIRST_NAME),
            lastName = text(root, ArchivePayloadFields.LAST_NAME),
            birthDate = date(root, ArchivePayloadFields.BIRTH_DATE),
            specialtyName = text(root, ArchivePayloadFields.SPECIALTY_NAME),
            studentNumber = text(root, ArchivePayloadFields.STUDENT_NUMBER),
            documents = documentIdentities(root),
            specialtyType = text(root, ArchivePayloadFields.SPECIALTY_TYPE),
            studyRightStartDate = date(root, ArchivePayloadFields.STUDY_RIGHT_START_DATE),
            degreeRegulation = text(root, ArchivePayloadFields.DEGREE_REGULATION),
            licensingDate = date(root, ArchivePayloadFields.LICENSING_DATE)
        )
    }

    private fun text(root: JsonNode, field: String): String? =
        root[field]?.takeUnless { it.isNull }?.let {
            requireValidPayload(it.isTextual)
            it.textValue()
        }

    private fun date(root: JsonNode, field: String): LocalDate? = try {
        text(root, field)?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) {
        throw ArchivePayloadException()
    }

    private fun identifier(root: JsonNode, field: String): Long {
        val value = requirePayloadValue(root[field])
        requireValidPayload(value.isIntegralNumber && value.canConvertToLong() && value.longValue() > 0)
        return value.longValue()
    }

    private fun documentIdentities(root: JsonNode): List<ArkistointiDocumentIdentity> {
        val manifest = requirePayloadValue(root[ArchivePayloadFields.DOCUMENTS])
        requireValidPayload(manifest.isArray)
        return try {
            mapper.treeToValue(manifest, Array<ArkistointiDocumentIdentity>::class.java).toList()
        } catch (_: JsonProcessingException) {
            throw ArchivePayloadException()
        }
    }

    private fun requireValidPayload(valid: Boolean) {
        if (!valid) throw ArchivePayloadException()
    }

    private fun <T : Any> requirePayloadValue(value: T?): T =
        value ?: throw ArchivePayloadException()
}
