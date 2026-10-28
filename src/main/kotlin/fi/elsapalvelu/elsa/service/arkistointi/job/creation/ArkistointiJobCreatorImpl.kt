package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArkistointiJobCreatorImpl(
    private val jobRepository: ArkistointiJobRepository,
    private val jobScheduler: ArkistointiJobScheduler,
    private val snapshotService: ArkistointiAsiakirjaSnapshotService
) : ArkistointiJobCreator {

    @Transactional
    override fun create(jobRequest: CreateArkistointiJobRequest): ArkistointiJob {
        validate(jobRequest)

        jobRepository.findByKey(jobRequest.key)?.let { return it }
        val job = ArkistointiJob(
            university = jobRequest.university,
            caseType = jobRequest.caseType,
            payload = jobRequest.payload,
            key = jobRequest.key,
            expectedDocumentCount = jobRequest.asiakirjat.size
        )
        jobRequest.asiakirjat.sortedBy { it.jarjestysnumero }.forEach { reference ->
            val snapshot = snapshotService.resolveReference(reference)
            job.lisaaAsiakirja(
                ArkistointiJobAsiakirja(
                    asiakirja = snapshot.asiakirja,
                    asiakirjatyyppi = reference.asiakirjatyyppi,
                    jarjestysnumero = reference.jarjestysnumero,
                    filename = snapshot.filename,
                    contentType = snapshot.contentType,
                    sha256 = snapshot.sha256
                )
            )
        }

        val saved = jobRepository.saveAndFlush(job)
        val jobId = requireNotNull(saved.id)
        check(jobScheduler.schedule(jobId)) { "Arkistointi-jobin ajastus epaonnistui" }
        return saved
    }

    private fun validate(pyynto: CreateArkistointiJobRequest) {
        require(pyynto.key.isNotBlank()) { "Arkistointi-jobin key ei saa olla tyhja" }
        require(pyynto.payload.isNotBlank()) { "Arkistointi-jobin payload ei saa olla tyhja" }
        require(pyynto.asiakirjat.isNotEmpty()) { "Arkistointi-jobilla on oltava asiakirjoja" }
        require(pyynto.asiakirjat.map { it.jarjestysnumero }.distinct().size == pyynto.asiakirjat.size) {
            "Arkistointi-jobin asiakirjojen jarjestysnumeroiden on oltava yksilollisia"
        }
    }
}
