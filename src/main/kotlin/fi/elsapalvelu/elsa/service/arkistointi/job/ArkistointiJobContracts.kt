package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
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
    fun process(jobId: UUID)
}

data class LoadedArkistointiAsiakirja(
    val viite: ArkistointiJobAsiakirja,
    val sisalto: ByteArray
)

interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiJobAsiakirja): LoadedArkistointiAsiakirja
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
