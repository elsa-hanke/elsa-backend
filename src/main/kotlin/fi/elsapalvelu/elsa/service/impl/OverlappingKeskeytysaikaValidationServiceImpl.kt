package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.OverlappingKeskeytysaikaValidationService
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import org.springframework.stereotype.Service

@Service
class OverlappingKeskeytysaikaValidationServiceImpl(
    private val keskeytysaikaMapper: KeskeytysaikaMapper,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository
) : OverlappingKeskeytysaikaValidationService {

    override fun validateKeskeytysaika(userId: String, keskeytysaikaDTO: KeskeytysaikaDTO): Boolean {
        tyoskentelyjaksoRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserIdEagerWithKeskeytykset(
            keskeytysaikaDTO.tyoskentelyjaksoId!!,
            userId
        )?.let {
            val keskeytykset =
                it.keskeytykset.filter { k -> k.id != keskeytysaikaDTO.id }.toList() + keskeytysaikaMapper.toEntity(
                    keskeytysaikaDTO
                )

            val minKeskeytysaikaDate = keskeytykset.minOf { k -> k.alkamispaiva!! }
            val maxKeskeytysaikaDate = keskeytykset.maxOf { k -> k.paattymispaiva!! }

            dates@ for (date in minKeskeytysaikaDate.datesUntil(maxKeskeytysaikaDate.plusDays(1))) {
                val keskeytyksetForCurrentDate = keskeytykset.filter { keskeytysaika ->
                    date.isInRange(keskeytysaika.alkamispaiva!!, keskeytysaika.paattymispaiva)
                }
                val overallKeskeytysaikaFactorForCurrentDate = keskeytyksetForCurrentDate.sumOf { k ->
                    k.osaaikaprosentti!!.toDouble() / 100.0
                }
                if (overallKeskeytysaikaFactorForCurrentDate > 1) {
                    return false
                }
            }
        } ?: return false

        return true
    }
}
