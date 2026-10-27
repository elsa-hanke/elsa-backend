package fi.elsapalvelu.elsa.service.arkistointi.louhi

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataRequest
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePerson
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.MissingResourceException

data class PreparedLouhiPackage(val path: Path, val yek: Boolean) {
    override fun toString() = "PreparedLouhiPackage(yek=$yek)"
}

/** Reuses the production SÄHKE serializer, but takes all person/document data from the saved submission. */
@Component
class LouhiAsyncPackageBuilder(
    private val mapper: ObjectMapper,
    private val configurationProvider: ArkistointiConfigurationProvider,
    private val metadataBuilder: SahkeMetadataBuilder,
    private val packageBuilder: SahkePakettiBuilder
) {
    fun build(submission: ArkistointiSubmission, directory: Path): PreparedLouhiPackage {
        val payload = try {
            payloadValue(mapper.readTree(submission.payload))
        } catch (_: JsonProcessingException) {
            throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
        }
        checkPayload(payload.isObject)
        checkPayload(payload.path("university").asText() == submission.university.name)
        fun text(field: String): String? = payload[field]?.takeUnless { it.isNull }?.let {
            checkPayload(it.isTextual)
            it.textValue()
        }
        fun date(field: String) = try {
            text(field)?.let(LocalDate::parse)
        } catch (_: DateTimeParseException) {
            throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
        }
        val approvalDate = payloadValue(date("approvalDate"))
        fun identifier(field: String): Long {
            val value = payloadValue(payload[field])
            checkPayload(value.isIntegralNumber && value.canConvertToLong() && value.longValue() > 0)
            return value.longValue()
        }
        val studyRightId = identifier("opintooikeusId")
        val caseId = if (submission.caseType == CaseType.VALMISTUMINEN) "valmistumispyyntoId" else "caseId"
        val savedCaseId = identifier(caseId)
        checkPayload(payload.path("yek").isBoolean)
        val configuration = configuration(submission)
        checkPayload(submission.asiakirjat.isNotEmpty())
        val documents = documents(submission, configuration)
        checkPayload(documents.map { it.asiakirja.nimi }.distinct().size == documents.size)
        val request = SahkeMetadataRequest(
            null, documents, savedCaseId.toString(), text("reviewer"), date("reviewDate"),
            text("approver"), approvalDate,
            SahkePerson(studyRightId, text("traineeName"), text("firstName"), text("lastName"),
                date("birthDate"), text("specialtyName"), text("studentNumber"), submission.university),
            approvalDate
        )
        val filename = packageBuilder.filename(configuration.case.type, text("firstName"), text("lastName"))
        checkPayload(filename.none { it == '/' || it == '\\' || it.isISOControl() })
        val destination = directory.resolve(filename)
        val metadata = try {
            metadataBuilder.build(request, configuration)
        } catch (_: MissingResourceException) {
            throw LouhiPackageException(LouhiPackageFailure.LOCALIZATION_MISSING)
        }
        packageBuilder.build(metadata, destination, true)
        return PreparedLouhiPackage(destination, payload["yek"].booleanValue())
    }

    private fun configuration(submission: ArkistointiSubmission): ArkistointiConfiguration {
        val configuration = try {
            configurationProvider.getConfiguration(submission.university, submission.caseType)
        } catch (_: IllegalArgumentException) {
            // The provider explicitly rejects a missing university/case configuration.
            throw LouhiPackageException(LouhiPackageFailure.CONFIGURATION_INVALID)
        }
        if (!configuration.metadata.zipMetadata) {
            throw LouhiPackageException(LouhiPackageFailure.CONFIGURATION_INVALID)
        }
        return configuration
    }

    private fun documents(submission: ArkistointiSubmission, configuration: ArkistointiConfiguration): List<RecordProperties> =
        submission.asiakirjat.sortedBy { it.jarjestysnumero }.map {
            // Detached value holders for the shared legacy serializer, never persisted or lazily loaded.
            checkPayload(it.filename.isNotBlank() && it.filename !in setOf(".", ".."))
            checkPayload(it.filename.none { character -> character == '/' || character == '\\' || character.isISOControl() })
            if (configuration.metadata.getDocumentMetadata(it.asiakirjatyyppi, configuration.case) == null) {
                throw LouhiPackageException(LouhiPackageFailure.CONFIGURATION_INVALID)
            }
            RecordProperties(
                Asiakirja(id = it.asiakirjaId, nimi = it.filename, tyyppi = it.contentType,
                    asiakirjaData = AsiakirjaData(data = it.sisalto)),
                it.asiakirjatyyppi
            )
        }

    private fun checkPayload(valid: Boolean) {
        if (!valid) throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
    }

    private fun <T : Any> payloadValue(value: T?): T =
        value ?: throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
}
