package fi.elsapalvelu.elsa.service.arkistointi.louhi

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Explicit JSON type checks preserve the saved payload contract without scalar coercion. */
@Component
class LouhiPayloadReader(private val mapper: ObjectMapper) {
    fun read(json: String, caseType: CaseType): LouhiArchivePayload {
        val root = try {
            requirePayloadValue(mapper.readTree(json))
        } catch (_: JsonProcessingException) {
            throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
        }
        requireValidPayload(root.isObject)
        val caseIdField = if (caseType == CaseType.VALMISTUMINEN) "valmistumispyyntoId" else "caseId"
        val yek = root.path("yek")
        requireValidPayload(yek.isBoolean)
        return LouhiArchivePayload(
            university = requirePayloadValue(text(root, "university")),
            studyRightId = identifier(root, "opintooikeusId"),
            caseId = identifier(root, caseIdField),
            yek = yek.booleanValue(),
            approvalDate = requirePayloadValue(date(root, "approvalDate")),
            reviewer = text(root, "reviewer"),
            reviewDate = date(root, "reviewDate"),
            approver = text(root, "approver"),
            traineeName = text(root, "traineeName"),
            firstName = text(root, "firstName"),
            lastName = text(root, "lastName"),
            birthDate = date(root, "birthDate"),
            specialtyName = text(root, "specialtyName"),
            studentNumber = text(root, "studentNumber"),
            documents = documentIdentities(root)
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
        throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
    }

    private fun identifier(root: JsonNode, field: String): Long {
        val value = requirePayloadValue(root[field])
        requireValidPayload(value.isIntegralNumber && value.canConvertToLong() && value.longValue() > 0)
        return value.longValue()
    }

    private fun documentIdentities(root: JsonNode): List<ArkistointiDocumentIdentity> {
        val manifest = requirePayloadValue(root["documents"])
        requireValidPayload(manifest.isArray)
        return try {
            mapper.treeToValue(manifest, Array<ArkistointiDocumentIdentity>::class.java).toList()
        } catch (_: JsonProcessingException) {
            throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
        }
    }

    private fun requireValidPayload(valid: Boolean) {
        if (!valid) throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
    }

    private fun <T : Any> requirePayloadValue(value: T?): T =
        value ?: throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
}
