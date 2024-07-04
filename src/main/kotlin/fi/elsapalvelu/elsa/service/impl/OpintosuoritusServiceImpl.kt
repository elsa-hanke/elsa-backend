package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.OpintosuoritusRepository
import fi.elsapalvelu.elsa.service.OpintosuoritusService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OpintosuoritusServiceImpl(
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintosuoritusMapper: OpintosuoritusMapper
) : OpintosuoritusService {

    @Transactional(readOnly = true)
    override fun getOpintosuorituksetByOpintooikeusId(opintooikeusId: Long): OpintosuorituksetDTO {
        val opintooikeus = opintooikeusRepository.findOneById(opintooikeusId)
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
            opintooikeus?.erikoistuvaLaakari?.id!!,
            YEK_ERIKOISALA_ID
        ).filter { it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_PATEVYYS }
        val opintosuorituksetList =
            (yekSuoritukset + opintosuoritusRepository.findAllByOpintooikeusId(opintooikeusId)).map(opintosuoritusMapper::toDto)
                .sortedByDescending { it.suorituspaiva }

        return OpintosuorituksetDTO(
            opintosuorituksetList,
            opintosuorituksetList.filter { opintosuoritus ->
                opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
            }.sumOf { johtamisopinto ->
                johtamisopinto.opintopisteet ?: 0.0
            },
            opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara,
            opintosuorituksetList.filter { opintosuoritus ->
                opintosuoritus.tyyppi?.nimi == OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS
            }.sumOf { sateilysuojakoulutus ->
                sateilysuojakoulutus.opintopisteet ?: 0.0
            },
            opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara
        )
    }

    @Transactional(readOnly = true)
    override fun getOpintosuorituksetByOpintooikeusIdAndTyyppi(
        opintooikeusId: Long, tyyppi: OpintosuoritusTyyppiEnum
    ): OpintosuorituksetDTO {
        val opintosuorituksetList =
            opintosuoritusRepository.findAllByOpintooikeusIdAndTyyppi(
                opintooikeusId, tyyppi
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

    override fun getTerveyskoulutusjaksoSuoritettu(opintooikeusId: Long, erikoistuvaLaakariId: Long): Boolean {
        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeusId).asSequence()
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
            erikoistuvaLaakariId,
            YEK_ERIKOISALA_ID
        ).asSequence()
        return (opintosuoritukset + yekSuoritukset).any {
            (it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO
                || it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO) && it.hyvaksytty
        }
    }

    override fun getTerveyskoulutusjaksoSuoritusPvm(opintooikeusId: Long, erikoistuvaLaakariId: Long): LocalDate? {
        val opintosuoritukset = opintosuoritusRepository.findAllByOpintooikeusId(opintooikeusId).asSequence()
        val yekSuoritukset = opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(
            erikoistuvaLaakariId,
            YEK_ERIKOISALA_ID
        ).asSequence()
        return (opintosuoritukset + yekSuoritukset).firstOrNull {
            (it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO
                || it.tyyppi?.nimi == OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO) && it.hyvaksytty
        }?.suorituspaiva
    }

}
