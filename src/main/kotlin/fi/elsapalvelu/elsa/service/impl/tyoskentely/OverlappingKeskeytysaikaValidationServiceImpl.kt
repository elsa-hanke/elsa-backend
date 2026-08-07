package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.tyoskentely.OverlappingKeskeytysaikaValidationService
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.KeskeytysaikaMapper
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class OverlappingKeskeytysaikaValidationServiceImpl(
    private val keskeytysaikaMapper: KeskeytysaikaMapper,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
) : OverlappingKeskeytysaikaValidationService {

    override fun validateKeskeytysaika(opintooikeusId: Long, keskeytysaikaDTO: KeskeytysaikaDTO): Boolean {
        tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusIdEagerWithKeskeytykset(
            keskeytysaikaDTO.tyoskentelyjaksoId.required(),
            opintooikeusId
        )?.let {
            val keskeytykset =
                it.keskeytykset.filter { k -> k.id != keskeytysaikaDTO.id }.toList() + keskeytysaikaMapper.toEntity(
                    keskeytysaikaDTO
                )

            val minKeskeytysaikaDate = keskeytykset.minOf { k -> k.alkamispaiva.required() }
            val maxKeskeytysaikaDate = keskeytykset.maxOf { k -> k.paattymispaiva.required() }

            dates@ for (date in minKeskeytysaikaDate.datesUntil(maxKeskeytysaikaDate.plusDays(1))) {
                val keskeytyksetForCurrentDate = keskeytykset.filter { keskeytysaika ->
                    date.isInRange(keskeytysaika.alkamispaiva.required(), keskeytysaika.paattymispaiva)
                }
                val overallKeskeytysaikaFactorForCurrentDate = keskeytyksetForCurrentDate.sumOf { k ->
                    k.poissaoloprosentti.required().toDouble() / 100.0
                }
                if (overallKeskeytysaikaFactorForCurrentDate > 1) {
                    return false
                }
            }
        } ?: return false

        return true
    }
}
