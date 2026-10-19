package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.time.Instant

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
    fun schedule(jobId: Long, executionTime: Instant = Instant.now()): Boolean
}

interface ArkistointiJobProcessor {
    fun process(execution: ArkistointiJobExecution)
}

/** Captured once when db-scheduler claims a task; never replace it with a newer database version. */
data class ArkistointiJobExecution(val taskName: String, val taskInstanceId: String, val version: Long) {
    val jobId: Long = requireNotNull(taskInstanceId.toLongOrNull()) { "Arkistointi-jobin tunniste ei ole numero" }
}

/** Control flow: another execution owns this task, or it has already been acknowledged. */
internal class StaleArkistointiExecutionException : RuntimeException("Archive execution is no longer current")

data class LoadedArkistointiAsiakirja(
    val viite: ArkistointiJobAsiakirja,
    val sisalto: ByteArray
)

interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiJobAsiakirja): LoadedArkistointiAsiakirja
}

data class ArkistointiSubmission(
    val jobId: Long,
    val key: String,
    val university: YliopistoEnum,
    val caseType: CaseType,
    val payload: String,
    val asiakirjat: List<LoadedArkistointiAsiakirja>
) {
    override fun toString() = "ArkistointiSubmission(" +
        "jobId=$jobId, " +
        "university=$university, " +
        "caseType=$caseType, " +
        "asiakirjoja=${asiakirjat.size})"
}
