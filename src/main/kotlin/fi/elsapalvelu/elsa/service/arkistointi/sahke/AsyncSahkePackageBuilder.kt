package fi.elsapalvelu.elsa.service.arkistointi.sahke

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfiguration
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.MissingResourceException

data class PreparedSahkePackage(val path: Path, val yek: Boolean) {
    override fun toString() = "PreparedSahkePackage(yek=$yek)"
}

/** Reuses the production SÄHKE serializer, but takes all person/document data from the saved submission. */
@Component
class AsyncSahkePackageBuilder(
    private val payloadReader: SahkePayloadReader,
    private val configurationProvider: ArkistointiConfigurationProvider,
    private val metadataBuilder: SahkeMetadataBuilder,
    private val packageBuilder: SahkePakettiBuilder
) {
    fun build(submission: ArkistointiSubmission, directory: Path): PreparedSahkePackage {
        val payload = payloadReader.read(submission.payload, submission.caseType)
        requireValidPayload(payload.university == submission.university.name)
        val configuration = requireConfiguration(submission)
        val identities = validateDocumentManifest(payload.documents, submission.asiakirjat)
        val records = createRecordProperties(submission.asiakirjat, configuration, identities)
        val request = createMetadataRequest(payload, records, submission)
        val destination = resolvePackagePath(directory, payload, configuration)
        val metadata = buildMetadata(request, configuration)
        packageBuilder.build(metadata, destination, true)
        return PreparedSahkePackage(destination, payload.yek)
    }

    private fun requireConfiguration(submission: ArkistointiSubmission): ArkistointiConfiguration {
        val configuration = try {
            configurationProvider.getConfiguration(submission.university, submission.caseType)
        } catch (_: IllegalArgumentException) {
            // The provider explicitly rejects a missing university/case configuration.
            throw SahkePackageException(SahkePackageFailure.CONFIGURATION_INVALID)
        }
        if (!configuration.metadata.zipMetadata) {
            throw SahkePackageException(SahkePackageFailure.CONFIGURATION_INVALID)
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
                throw SahkePackageException(SahkePackageFailure.CONFIGURATION_INVALID)
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
        payload: SahkeArchivePayload,
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
        payload: SahkeArchivePayload,
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
            throw SahkePackageException(SahkePackageFailure.LOCALIZATION_MISSING)
        }

    private fun requireValidPayload(valid: Boolean) {
        if (!valid) throw SahkePackageException(SahkePackageFailure.PAYLOAD_INVALID)
    }

    private fun <T : Any> requirePayloadValue(value: T?): T =
        value ?: throw SahkePackageException(SahkePackageFailure.PAYLOAD_INVALID)
}
