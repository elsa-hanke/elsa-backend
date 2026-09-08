package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArkistointiJobCreatorImpl(
    private val jobRepository: ArkistointiJobRepository,
    private val jobScheduler: ArkistointiJobScheduler,
    private val pdfA2bService: PdfA2bService
) : ArkistointiJobCreator {

    @Transactional
    override fun create(pyynto: CreateArkistointiJobRequest): ArkistointiJob {
        require(pyynto.key.isNotBlank()) { "Arkistointi-jobin key ei saa olla tyhja" }
        require(pyynto.payload.isNotBlank()) { "Arkistointi-jobin payload ei saa olla tyhja" }
        require(pyynto.asiakirjat.isNotEmpty()) { "Arkistointi-jobilla on oltava asiakirjoja" }
        require(pyynto.asiakirjat.map { it.jarjestysnumero }.distinct().size == pyynto.asiakirjat.size) {
            "Arkistointi-jobin asiakirjojen jarjestysnumeroiden on oltava yksilollisia"
        }

        jobRepository.findByKey(pyynto.key)?.let { return it }

        pyynto.asiakirjat.forEach(::validateReference)
        val job = ArkistointiJob(
            university = pyynto.university,
            caseType = pyynto.caseType,
            payload = pyynto.payload,
            key = pyynto.key
        )
        pyynto.asiakirjat.sortedBy { it.jarjestysnumero }.forEach { reference ->
            job.lisaaAsiakirja(
                ArkistointiJobAsiakirja(
                    asiakirja = reference.asiakirja,
                    asiakirjatyyppi = reference.asiakirjatyyppi,
                    jarjestysnumero = reference.jarjestysnumero,
                    filename = reference.filename,
                    contentType = reference.contentType,
                    sha256 = reference.sha256.lowercase()
                )
            )
        }

        val saved = jobRepository.saveAndFlush(job)
        val jobId = requireNotNull(saved.id)
        check(jobScheduler.schedule(jobId)) { "Arkistointi-jobin ajastus epaonnistui" }
        return saved
    }

    private fun validateReference(reference: ArkistointiAsiakirjaReference) {
        val identifier = reference.filename
        if (
            reference.jarjestysnumero < 0 ||
            reference.filename.isBlank() ||
            reference.contentType != MediaType.APPLICATION_PDF_VALUE
        ) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid(identifier))
        }
        val data = reference.asiakirja.asiakirjaData?.data
            ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        if (data.isEmpty()) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.empty(identifier))
        }
        if (reference.asiakirja.id == null) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing(identifier))
        }
        if (!pdfA2bService.isPdfA2b(data)) {
            throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.notPdfA2b(identifier))
        }
        if (!ArkistointiChecksum.sha256(data).equals(reference.sha256, ignoreCase = true)) {
            throw ArkistointiAsiakirjaException(
                ArkistointiAsiakirjaErrors.checksumMismatch(identifier)
            )
        }
    }
}
