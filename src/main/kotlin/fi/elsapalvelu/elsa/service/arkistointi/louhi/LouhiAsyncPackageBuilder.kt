package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataRequest
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataResult
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePerson
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.MissingResourceException

data class PreparedLouhiPackage(val path: Path, val yek: Boolean) {
    override fun toString() = "PreparedLouhiPackage(yek=$yek)"
}

/** Reuses the production SÄHKE serializer, but takes all person/document data from the saved submission. */
@Component
class LouhiAsyncPackageBuilder(
    private val payloadReader: LouhiPayloadReader,
    private val configurationProvider: ArkistointiConfigurationProvider,
    private val metadataBuilder: SahkeMetadataBuilder,
    private val packageBuilder: SahkePakettiBuilder
) {
    fun build(submission: ArkistointiSubmission, directory: Path): PreparedLouhiPackage {
        val payload = payloadReader.read(submission.payload, submission.caseType)
        requireValidPayload(payload.university == submission.university.name)
        val configuration = requireConfiguration(submission)
        val identities = validateDocumentManifest(payload.documents, submission.asiakirjat)
        val records = createRecordProperties(submission.asiakirjat, configuration, identities)
        val request = createMetadataRequest(payload, records, submission)
        val destination = resolvePackagePath(directory, payload, configuration)
        val metadata = buildMetadata(request, configuration)
        packageBuilder.build(metadata, destination, true)
        return PreparedLouhiPackage(destination, payload.yek)
    }

    private fun requireConfiguration(submission: ArkistointiSubmission): ArkistointiConfiguration {
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

    private fun validateDocumentManifest(
        identities: List<ArkistointiDocumentIdentity>,
        documents: List<LoadedArkistointiAsiakirja>
    ): Map<Long, ArkistointiDocumentIdentity> {
        requireValidPayload(documents.isNotEmpty() && identities.size == documents.size)
        requireValidPayload(identities.all { it.sourceDocumentId > 0 && it.snapshotDocumentId > 0 && it.jarjestysnumero >= 0 })
        requireValidPayload(identities.map { it.snapshotDocumentId }.distinct().size == identities.size)
        requireValidPayload(identities.map { it.jarjestysnumero }.distinct().size == identities.size)
        requireValidPayload(documents.map { it.asiakirjaId }.distinct().size == identities.size)
        val bySnapshot = identities.associateBy { it.snapshotDocumentId }
        documents.forEach { document ->
            val identity = requirePayloadValue(bySnapshot[document.asiakirjaId])
            requireValidPayload(identity.recordType == document.asiakirjatyyppi && identity.jarjestysnumero == document.jarjestysnumero)
        }
        return bySnapshot
    }

    private fun createRecordProperties(
        documents: List<LoadedArkistointiAsiakirja>,
        configuration: ArkistointiConfiguration,
        identities: Map<Long, ArkistointiDocumentIdentity>
    ): List<RecordProperties> {
        val records = documents.sortedBy { it.jarjestysnumero }.map { document ->
            requireValidPayload(document.filename.isNotBlank() && document.filename !in setOf(".", ".."))
            requireValidPayload(document.filename.none { it == '/' || it == '\\' || it.isISOControl() })
            if (configuration.metadata.getDocumentMetadata(document.asiakirjatyyppi, configuration.case) == null) {
                throw LouhiPackageException(LouhiPackageFailure.CONFIGURATION_INVALID)
            }
            RecordProperties(
                // In-memory holders only: legacy XML uses the original ID, bytes come from the snapshot.
                Asiakirja(
                    id = identities.getValue(document.asiakirjaId).sourceDocumentId,
                    nimi = document.filename,
                    tyyppi = document.contentType,
                    asiakirjaData = AsiakirjaData(data = document.sisalto)
                ),
                document.asiakirjatyyppi
            )
        }
        requireValidPayload(records.map { it.asiakirja.nimi }.distinct().size == records.size)
        return records
    }

    private fun createMetadataRequest(
        payload: LouhiArchivePayload,
        records: List<RecordProperties>,
        submission: ArkistointiSubmission
    ) = SahkeMetadataRequest(
        opintooikeus = null,
        asiakirjat = records,
        caseId = payload.caseId.toString(),
        tarkastaja = payload.reviewer,
        tarkastusPaiva = payload.reviewDate,
        hyvaksyja = payload.approver,
        hyvaksymisPaiva = payload.approvalDate,
        savedPerson = SahkePerson(
            studyRightId = payload.studyRightId,
            name = payload.traineeName,
            firstName = payload.firstName,
            lastName = payload.lastName,
            birthDate = payload.birthDate,
            specialty = payload.specialtyName,
            studentNumber = payload.studentNumber,
            university = submission.university
        ),
        created = payload.approvalDate
    )

    private fun resolvePackagePath(
        directory: Path,
        payload: LouhiArchivePayload,
        configuration: ArkistointiConfiguration
    ): Path {
        val filename = packageBuilder.filename(configuration.case.type, payload.firstName, payload.lastName)
        requireValidPayload(filename.none { it == '/' || it == '\\' || it.isISOControl() })
        return directory.resolve(filename)
    }

    private fun buildMetadata(request: SahkeMetadataRequest, configuration: ArkistointiConfiguration): SahkeMetadataResult =
        try {
            metadataBuilder.build(request, configuration)
        } catch (_: MissingResourceException) {
            throw LouhiPackageException(LouhiPackageFailure.LOCALIZATION_MISSING)
        }

    private fun requireValidPayload(valid: Boolean) {
        if (!valid) throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
    }

    private fun <T : Any> requirePayloadValue(value: T?): T =
        value ?: throw LouhiPackageException(LouhiPackageFailure.PAYLOAD_INVALID)
}
