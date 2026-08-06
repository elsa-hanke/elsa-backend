package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.tyoskentely.KeskeytysaikaService
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KeskeytysaikaServiceImpl(
    private val keskeytysaikaRepository: KeskeytysaikaRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val keskeytysaikaMapper: KeskeytysaikaMapper
) : KeskeytysaikaService {

    override fun save(keskeytysaikaDTO: KeskeytysaikaDTO, opintooikeusId: Long): KeskeytysaikaDTO? {
        tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(
            keskeytysaikaDTO.tyoskentelyjaksoId!!,
            opintooikeusId
        )
            ?.let { tyoskentelyjakso ->
                if (tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon) {
                    throw ValidationException("Terveyskeskuskoulutusjaksoon liitetyn työskentelyjakson poissaoloja ei voi päivittää")
                }
                if (tyoskentelyjakso.alkamispaiva!!.isBefore(keskeytysaikaDTO.alkamispaiva) || tyoskentelyjakso.alkamispaiva!!.isEqual(
                        keskeytysaikaDTO.alkamispaiva
                    )
                ) {
                    if (
                        tyoskentelyjakso.paattymispaiva != null &&
                        tyoskentelyjakso.paattymispaiva!!.isBefore(keskeytysaikaDTO.paattymispaiva)
                    ) {
                        return null
                    }

                    var keskeytysaika = keskeytysaikaMapper.toEntity(keskeytysaikaDTO)
                    keskeytysaika = keskeytysaikaRepository.save(keskeytysaika)
                    return keskeytysaikaMapper.toDto(keskeytysaika)
                }
            }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoOpintooikeusId(
        opintooikeusId: Long
    ): List<KeskeytysaikaDTO> {
        return keskeytysaikaRepository.findAllByTyoskentelyjaksoOpintooikeusIdOrderByAlkamispaivaAsc(opintooikeusId)
            .map(keskeytysaikaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, opintooikeusId: Long): KeskeytysaikaDTO? {
        keskeytysaikaRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(id, opintooikeusId)
            ?.let {
                return keskeytysaikaMapper.toDto(it)
            }

        return null
    }

    override fun delete(id: Long, opintooikeusId: Long) {
        keskeytysaikaRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(id, opintooikeusId)
            ?.let {
                tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(it.id!!, opintooikeusId)
                    ?.let { tyoskentelyjakso ->
                        if (tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon) {
                            throw ValidationException("Terveyskeskuskoulutusjaksoon liitetyn työskentelyjakson poissaoloja ei voi päivittää")
                        }
                    }

                keskeytysaikaRepository.deleteById(id)
            }
    }
}
