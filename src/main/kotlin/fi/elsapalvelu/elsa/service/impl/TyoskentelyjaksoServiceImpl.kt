package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi.*
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi.*
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

@Service
@Transactional
class TyoskentelyjaksoServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kuntaRepository: KuntaRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper
) : TyoskentelyjaksoService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String
    ): TyoskentelyjaksoDTO? {
        log.debug("Request to save Tyoskentelyjakso : $tyoskentelyjaksoDTO")

        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
            if (tyoskentelyjaksoDTO.erikoistuvaLaakariId == null) {
                tyoskentelyjaksoDTO.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.id
            }

            if (tyoskentelyjaksoDTO.erikoistuvaLaakariId == kirjautunutErikoistuvaLaakari.id) {
                // Jos päivitetään olemassa olevaa, tarkistetaan sallitaan vain päättymispäivä muokkaus.
                var tyoskentelyjakso = if (tyoskentelyjaksoDTO.id != null) {
                    tyoskentelyjaksoRepository.findByIdOrNull(tyoskentelyjaksoDTO.id)?.let {
                        it.paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva
                        it
                    } ?: return null
                } else {
                    val newTyoskentelyjakso = tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO)
                    newTyoskentelyjakso.tyoskentelypaikka!!.kunta = kuntaRepository
                        .findByIdOrNull(tyoskentelyjaksoDTO.tyoskentelypaikka!!.kuntaId)
                    newTyoskentelyjakso
                }

                // Tarkistetaan päättymispäivä suoritusarvioinneille
                tyoskentelyjakso.suoritusarvioinnit.forEach {
                    if (it.tapahtumanAjankohta!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                        return null
                    }
                }

                // Tarkistetaan päättymispäivä suoritemerkinnöille
                tyoskentelyjakso.suoritemerkinnat.forEach {
                    if (it.suorituspaiva!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                        return null
                    }
                }

                // Tarkistetaan päättymispäivä keskeytyksille
                tyoskentelyjakso.keskeytykset.forEach {
                    if (it.paattymispaiva!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                        return null
                    }
                }

                tyoskentelyjakso = tyoskentelyjaksoRepository.save(tyoskentelyjakso)
                return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
            }
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<TyoskentelyjaksoDTO> {
        log.debug("Request to get list of Tyoskentelyjakso by user id : $userId")

        return tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
            .mapTo(mutableListOf(), tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO? {
        log.debug("Request to get Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
                    }
                }
            }
        }

        return null
    }

    override fun delete(id: Long, userId: String) {
        log.debug("Request to delete Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (
                        kirjautunutErikoistuvaLaakari == it &&
                        !tyoskentelyjakso.isSuoritusarvioinnitNotEmpty()
                    ) {
                        tyoskentelyjaksoRepository.deleteById(id)
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getTilastot(userId: String): TyoskentelyjaksotTilastotDTO {
        var tyoskentelyaikaYhteensa = 0.0
        var terveyskeskusSuoritettu = 0.0
        var yliopistosairaalaSuoritettu = 0.0
        var yliopistosairaaloidenUlkopuolinenSuoritettu = 0.0

        var nykyiselleErikoisalalleSuoritettu = 0.0
        var hyvaksyttyToiselleErikoisalalleSuoritettu = 0.0

        val tyoskentelyjaksotSuoritettu = mutableSetOf<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>()
        val kaytannonKoulutusSuoritettuMap = KaytannonKoulutusTyyppi.values().map { it to 0.0 }.toMap().toMutableMap()
        val tyoskentelyjaksot = tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        tyoskentelyjaksot.map { tyoskentelyjakso ->
            val daysBetween = ChronoUnit.DAYS.between(
                tyoskentelyjakso.alkamispaiva,
                tyoskentelyjakso.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
            ) + 1

            // Ei huomioida tulevaisuuden jaksoja
            if (daysBetween > 0) {
                val factor = (tyoskentelyjakso.osaaikaprosentti!!.toDouble() / 100.0)
                val result = factor * daysBetween

                // TODO: Vähennetään poissaolot

                // Summataan suoritettu aika koulutustyypettäin
                when (tyoskentelyjakso.tyoskentelypaikka!!.tyyppi!!) {
                    TERVEYSKESKUS -> terveyskeskusSuoritettu += result
                    YLIOPISTOLLINEN_SAIRAALA -> yliopistosairaalaSuoritettu += result
                    else -> yliopistosairaaloidenUlkopuolinenSuoritettu += result
                }

                // Summataan suoritettu aika käytännön koulutuksettain
                when (tyoskentelyjakso.kaytannonKoulutus!!) {
                    OMAN_ERIKOISALAN_KOULUTUS ->
                        kaytannonKoulutusSuoritettuMap[OMAN_ERIKOISALAN_KOULUTUS] =
                            kaytannonKoulutusSuoritettuMap[OMAN_ERIKOISALAN_KOULUTUS]!! + result
                    OMAA_ERIKOISALAA_TUKEVA_KOULUTUS ->
                        kaytannonKoulutusSuoritettuMap[OMAA_ERIKOISALAA_TUKEVA_KOULUTUS] =
                            kaytannonKoulutusSuoritettuMap[OMAA_ERIKOISALAA_TUKEVA_KOULUTUS]!! + result
                    TUTKIMUSTYO ->
                        kaytannonKoulutusSuoritettuMap[TUTKIMUSTYO] =
                            kaytannonKoulutusSuoritettuMap[TUTKIMUSTYO]!! + result
                    TERVEYSKESKUSTYO ->
                        kaytannonKoulutusSuoritettuMap[TERVEYSKESKUSTYO] =
                            kaytannonKoulutusSuoritettuMap[TERVEYSKESKUSTYO]!! + result
                }

                // Summataan nykyiselle tai hyväksytylle erikoisalalle
                if (tyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan) {
                    hyvaksyttyToiselleErikoisalalleSuoritettu += result
                } else {
                    nykyiselleErikoisalalleSuoritettu += result
                }

                // Summataan työskentelyaika yhteensä
                tyoskentelyaikaYhteensa += result

                // Kootaan työskentelyjaksojen suoritetut työskentelyajat
                tyoskentelyjaksotSuoritettu.add(
                    TyoskentelyjaksotTilastotTyoskentelyjaksotDTO(
                        id = tyoskentelyjakso.id!!,
                        suoritettu = result
                    )
                )
            }
        }

        val erikoisala = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.erikoisala
        val yhteensaVaadittuVahintaan = erikoisala?.kaytannonKoulutuksenVahimmaispituus ?: 0.0
        val arvioErikoistumiseenHyvaksyttavista =
            min(yhteensaVaadittuVahintaan / 2, hyvaksyttyToiselleErikoisalalleSuoritettu) +
                nykyiselleErikoisalalleSuoritettu

        return TyoskentelyjaksotTilastotDTO(
            tyoskentelyaikaYhteensa = tyoskentelyaikaYhteensa,
            arvioErikoistumiseenHyvaksyttavista = arvioErikoistumiseenHyvaksyttavista,
            arvioPuuttuvastaKoulutuksesta = max(0.0, yhteensaVaadittuVahintaan - arvioErikoistumiseenHyvaksyttavista),
            koulutustyypit = TyoskentelyjaksotTilastotKoulutustyypitDTO(
                terveyskeskusVaadittuVahintaan = erikoisala?.terveyskeskuskoulutusjaksonVahimmaispituus ?: 0.0,
                terveyskeskusSuoritettu = terveyskeskusSuoritettu,
                yliopistosairaalaVaadittuVahintaan = erikoisala?.yliopistosairaalajaksonVahimmaispituus ?: 0.0,
                yliopistosairaalaSuoritettu = yliopistosairaalaSuoritettu,
                yliopistosairaaloidenUlkopuolinenVaadittuVahintaan =
                    erikoisala?.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus ?: 0.0,
                yliopistosairaaloidenUlkopuolinenSuoritettu = yliopistosairaaloidenUlkopuolinenSuoritettu,
                yhteensaVaadittuVahintaan = yhteensaVaadittuVahintaan,
                yhteensaSuoritettu = arvioErikoistumiseenHyvaksyttavista
            ),
            kaytannonKoulutus = KaytannonKoulutusTyyppi.values().map {
                TyoskentelyjaksotTilastotKaytannonKoulutusDTO(
                    kaytannonKoulutus = it,
                    suoritettu = kaytannonKoulutusSuoritettuMap[it]!!
                )
            }
                .toMutableSet(),
            tyoskentelyjaksot = tyoskentelyjaksotSuoritettu
        )
    }
}
