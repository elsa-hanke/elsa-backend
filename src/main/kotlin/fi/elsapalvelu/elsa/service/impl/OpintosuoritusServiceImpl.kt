package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.OpintosuoritusRepository
import fi.elsapalvelu.elsa.service.OpintosuoritusService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpintosuoritusServiceImpl(
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintosuoritusMapper: OpintosuoritusMapper
) : OpintosuoritusService {

    @Transactional(readOnly = true)
    override fun getOpintosuorituksetByOpintooikeusId(opintooikeusId: Long): OpintosuorituksetDTO {
        val opintosuorituksetList =
            opintosuoritusRepository.findAllByOpintooikeusId(opintooikeusId).map(opintosuoritusMapper::toDto)
                .sortedByDescending { it.suorituspaiva }
        val opintooikeus = opintooikeusRepository.findOneById(opintooikeusId)

        return OpintosuorituksetDTO(
            opintosuorituksetList,
            opintosuorituksetList.filter { opintosuoritus ->
                opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
            }.sumOf { johtamisopinto ->
                johtamisopinto.opintopisteet ?: 0.0
            },
            opintooikeus?.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
            opintosuorituksetList.filter { opintosuoritus ->
                opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS
            }.sumOf { sateilysuojakoulutus ->
                sateilysuojakoulutus.opintopisteet ?: 0.0
            },
            opintooikeus?.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara
        )
    }

    @Transactional(readOnly = true)
    override fun getOpintosuorituksetByOpintooikeusIdAndTyyppiId(
        opintooikeusId: Long, tyyppiId: Long
    ): OpintosuorituksetDTO {
        val opintosuorituksetList =
            opintosuoritusRepository.findAllByOpintooikeusIdAndTyyppiId(
                opintooikeusId, tyyppiId
            ).map(opintosuoritusMapper::toDto).sortedByDescending { it.suorituspaiva }
        val opintooikeus = opintooikeusRepository.findOneById(opintooikeusId)

        return OpintosuorituksetDTO(
            opintosuorituksetList,
            opintosuorituksetList.filter { opintosuoritus ->
                opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
            }.sumOf { johtamisopinto ->
                johtamisopinto.opintopisteet ?: 0.0
            },
            opintooikeus?.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
            opintosuorituksetList.filter { opintosuoritus ->
                opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS
            }.sumOf { sateilysuojakoulutus ->
                sateilysuojakoulutus.opintopisteet ?: 0.0
            },
            opintooikeus?.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara
        )
    }

}
