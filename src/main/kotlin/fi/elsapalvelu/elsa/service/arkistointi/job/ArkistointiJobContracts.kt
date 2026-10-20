package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.time.Instant
import java.util.UUID

data class ArkistointiAsiakirjaReference(
    val asiakirja: Asiakirja,
    val asiakirjatyyppi: RecordType,
    val jarjestysnumero: Int,
    val filename: String,
    val contentType: String,
    val sha256: String
)

data class CreateArkistointiJobRequest(
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    /** Local business key for finding the existing job, not the external delivery identity. */
    val key: String,
    val asiakirjat: List<ArkistointiAsiakirjaReference>
) {
    override fun toString() = "CreateArkistointiJobRequest(" +
        "university=$university, " +
        "caseType=$caseType, " +
        "asiakirjoja=${asiakirjat.size})"
}

interface ArkistointiJobCreator {
    fun create(pyynto: CreateArkistointiJobRequest): ArkistointiJob
}

interface ArkistointiJobScheduler {
    fun schedule(jobId: UUID, executionTime: Instant = Instant.now()): Boolean
}

interface ArkistointiJobProcessor {
    fun process(execution: ArkistointiJobExecution)
}

/** Captured once when db-scheduler claims a task; never replace it with a newer database version. */
data class ArkistointiJobExecution(val taskName: String, val taskInstanceId: String, val version: Long) {
    val jobId: UUID = try {
        UUID.fromString(taskInstanceId).also {
            require(it.toString().equals(taskInstanceId, ignoreCase = true))
        }
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Arkistointi-jobin tunniste ei ole UUID")
    }
}

/** Control flow: another execution owns this task, or it has already been acknowledged. */
internal class StaleArkistointiExecutionException : RuntimeException("Archive execution is no longer current")

/** Detached scalar metadata only: preparation never loads document bytes. */
data class PreparedArkistointiJob(
    val jobId: UUID,
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    val asiakirjat: List<ArkistointiDocumentDescriptor>
) {
    override fun toString() = "PreparedArkistointiJob(jobId=$jobId, asiakirjoja=${asiakirjat.size})"
}

data class ArkistointiDocumentDescriptor(
    val asiakirjaId: Long,
    val asiakirjatyyppi: RecordType,
    val jarjestysnumero: Int,
    val filename: String,
    val contentType: String,
    val sha256: String
) {
    override fun toString() = "ArkistointiDocumentDescriptor(asiakirjaId=$asiakirjaId)"
}

/** Must be called while the job reference is attached; no entity escapes the transaction. */
internal fun ArkistointiJobAsiakirja.toDescriptor() = ArkistointiDocumentDescriptor(
    asiakirja?.id ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing()),
    asiakirjatyyppi.required(), jarjestysnumero.required(), filename.required(), contentType.required(),
    sha256 ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch())
)

data class LoadedArkistointiAsiakirja(
    val asiakirjaId: Long,
    val asiakirjatyyppi: RecordType,
    val jarjestysnumero: Int,
    val filename: String,
    val contentType: String,
    val sha256: String,
    val sisalto: ByteArray
) {
    override fun toString() = "LoadedArkistointiAsiakirja(asiakirjaId=$asiakirjaId)"
}

interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiDocumentDescriptor): LoadedArkistointiAsiakirja
}

data class ArkistointiSubmission(
    val jobId: UUID,
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    val asiakirjat: List<LoadedArkistointiAsiakirja>
) {
    /** The saved job ID is the delivery key, reused for every automatic and manual attempt. */
    val key: UUID get() = jobId

    override fun toString() = "ArkistointiSubmission(" +
        "jobId=$jobId, " +
        "university=$university, " +
        "caseType=$caseType, " +
        "asiakirjoja=${asiakirjat.size})"
}
