package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.arviointi.ErikoistujienSeurantaQueryService
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistujanEteneminenDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistujanEteneminenVirkailijaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistujienSeurantaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaErikoisalatPerYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KoulutettavanEteneminenDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EtusivunSeurantapalvelu(
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistujienSeurantaQueryService: ErikoistujienSeurantaQueryService,
    private val edistymispalvelu: EtusivunEdistymispalvelu
) {

    fun haeRajaimet(kayttajatunnus: String): ErikoistujienSeurantaDTO {
        val kayttaja = kayttajaRepository.findOneByUserId(kayttajatunnus).orElse(null)
            ?: return ErikoistujienSeurantaDTO()
        val yliopistojenErikoisalat = kayttaja.yliopistotAndErikoisalat.groupBy { it.yliopisto }.map { (yliopisto, oikeudet) ->
            KayttajaErikoisalatPerYliopistoDTO(
                yliopistoNimi = yliopisto?.nimi.toString(),
                erikoisalat = oikeudet.map { it.erikoisala?.nimi.required() }.sorted()
            )
        }
        return ErikoistujienSeurantaDTO(
            kayttajaYliopistoErikoisalat = yliopistojenErikoisalat,
            erikoisalat = yliopistojenErikoisalat.flatMap { it.erikoisalat.required() }.sorted().toMutableSet()
        )
    }

    fun haeErikoistujatVastuuhenkilolle(
        kayttajatunnus: String, hakuehdot: ErikoistujanEteneminenCriteria, sivutus: Pageable
    ): Page<ErikoistujanEteneminenDTO>? {
        val kayttaja = kayttajaRepository.findOneByUserId(kayttajatunnus).orElse(null) ?: return null
        return erikoistujienSeurantaQueryService.findByErikoisalaAndYliopisto(
            hakuehdot, sivutus, kayttaja.user?.langKey, kayttaja.yliopistotAndErikoisalat
        ).map(edistymispalvelu::muodostaErikoistujanEdistyminen)
    }

    fun haeKoulutettavatVastuuhenkilolle(
        kayttajatunnus: String, hakuehdot: ErikoistujanEteneminenCriteria, sivutus: Pageable
    ): Page<KoulutettavanEteneminenDTO>? {
        val kayttaja = kayttajaRepository.findOneByUserId(kayttajatunnus).orElse(null) ?: return null
        return erikoistujienSeurantaQueryService.findByErikoisalaAndYliopisto(
            hakuehdot, sivutus, kayttaja.user?.langKey, kayttaja.yliopistotAndErikoisalat
        ).map(edistymispalvelu::muodostaKoulutettavanEdistyminen)
    }

    fun haeErikoistujatKouluttajalle(
        kayttajatunnus: String, hakuehdot: ErikoistujanEteneminenCriteria, sivutus: Pageable
    ): Page<ErikoistujanEteneminenDTO>? {
        val kayttaja = kayttajaRepository.findOneByUserId(kayttajatunnus).orElse(null) ?: return null
        return erikoistujienSeurantaQueryService.findByKouluttajaValtuutus(
            hakuehdot, sivutus, kayttaja.id.required()
        ).map(edistymispalvelu::muodostaErikoistujanEdistyminen)
    }

    fun haeErikoistujatVirkailijalle(
        kayttajatunnus: String, hakuehdot: ErikoistujanEteneminenCriteria, sivutus: Pageable
    ): Page<ErikoistujanEteneminenVirkailijaDTO>? {
        val kayttaja = kayttajaRepository.findOneByUserId(kayttajatunnus).orElse(null) ?: return null
        // Opintohallinnon virkailija toimii vain yhden yliopiston alla.
        val yliopisto = kayttaja.yliopistot.firstOrNull() ?: return null
        return erikoistujienSeurantaQueryService.findByCriteriaAndYliopistoId(
            hakuehdot, sivutus, yliopisto.id.required(), kayttaja.user?.langKey, YEK_ERIKOISALA_ID
        ).map(edistymispalvelu::muodostaVirkailijanEdistyminen)
    }

    fun haeKoulutettavatVirkailijalle(
        kayttajatunnus: String, hakuehdot: ErikoistujanEteneminenCriteria, sivutus: Pageable
    ): Page<KoulutettavanEteneminenDTO>? {
        val kayttaja = kayttajaRepository.findOneByUserId(kayttajatunnus).orElse(null) ?: return null
        val yliopisto = kayttaja.yliopistot.firstOrNull() ?: return null
        return erikoistujienSeurantaQueryService.findByCriteriaAndYliopistoIdAndErikoisalaId(
            hakuehdot, sivutus, yliopisto.id.required(), YEK_ERIKOISALA_ID, kayttaja.user?.langKey
        ).map(edistymispalvelu::muodostaKoulutettavanEdistyminen)
    }
}
