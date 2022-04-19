package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ErikoisalaSisuTutkintoohjelma
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaImportService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SisuTutkintoohjelmaImportServiceImpl(
    private val erikoisalaRepository: ErikoisalaRepository
) : SisuTutkintoohjelmaImportService {
    override fun import(qualifications: Qualifications) {
        qualifications.entities?.asSequence()
            ?.filter { it.code != null && it.code.startsWith("e") }
            ?.groupBy { it.code }
            ?.mapValues { it.value }
            ?.forEach {
                it.value
                    .flatMap { e -> e.requirementCollections ?: listOf() }
                    .flatMap { r -> r.degreeProgrammeGroupIds?.toSet() ?: listOf() }
                    .toSet().let { ids ->
                        erikoisalaRepository.findOneByVirtaPatevyyskoodi(it.key!!)?.let { erikoisala ->
                            erikoisala.sisuTutkintoohjelmat.removeIf { s -> s.tutkintoohjelmaId !in ids }
                            ids.map { id ->
                                if (erikoisala.sisuTutkintoohjelmat.find { s -> s.tutkintoohjelmaId == id } == null) {
                                    val erikoisalaSisuTutkintoohjelma = ErikoisalaSisuTutkintoohjelma(
                                        tutkintoohjelmaId = id,
                                        erikoisala = erikoisala
                                    )
                                    erikoisala.sisuTutkintoohjelmat.add(erikoisalaSisuTutkintoohjelma)
                                }
                            }
                        }
                    }
            }
    }
}
