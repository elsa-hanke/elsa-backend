package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi.*
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi.*
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val erikoisalaRepository: ErikoisalaRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper,
    private val asiakirjaMapper: AsiakirjaMapper
) : TyoskentelyjaksoService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>
    ): TyoskentelyjaksoDTO? {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
            ?.let { kirjautunutErikoistuvaLaakari ->
                tyoskentelyjaksoDTO.apply {
                    erikoistuvaLaakariId = erikoistuvaLaakariId ?: kirjautunutErikoistuvaLaakari.id
                }
                kirjautunutErikoistuvaLaakari.takeIf { it.id == tyoskentelyjaksoDTO.erikoistuvaLaakariId }
                    ?.let {
                        tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO).apply {
                            this.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
                            tyoskentelypaikka?.kunta =
                                kuntaRepository.findByIdOrNull(tyoskentelyjaksoDTO.tyoskentelypaikka!!.kuntaId)
                            omaaErikoisalaaTukeva =
                                tyoskentelyjaksoDTO.omaaErikoisalaaTukeva.takeIf {
                                    kaytannonKoulutus == OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
                                }
                                    ?.let {
                                        erikoisalaRepository.findByIdOrNull(it.id)
                                    }
                        }.takeIf { isValidTyoskentelyjakso(it) }?.let { tyoskentelyjakso ->
                            mapAsiakirjat(
                                tyoskentelyjakso,
                                newAsiakirjat,
                                null,
                                kirjautunutErikoistuvaLaakari
                            )
                        }
                    }
            }?.let {
                tyoskentelyjaksoRepository.save(it)?.let { saved ->
                    return tyoskentelyjaksoMapper.toDto(saved)
                }
            }

        return null
    }

    override fun update(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): TyoskentelyjaksoDTO? {
        tyoskentelyjaksoDTO.id?.let { id ->
            tyoskentelyjaksoRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)
                ?.takeIf { tyoskentelyjaksoDTO.erikoistuvaLaakariId == it.erikoistuvaLaakari?.id }
                ?.let { tyoskentelyjakso ->
                    // Jos työskentelyjaksolle on lisätty arviointeja tai arviointipyyntöjä, sallitaan vain
                    // päättymispäivän muokkaus.
                    tyoskentelyjakso.takeIf { it.isSuoritusarvioinnitNotEmpty() }
                        ?.apply {
                            paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva
                        } ?: tyoskentelyjakso.let {
                        val updatedTyoskentelyjakso =
                            tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO)
                        tyoskentelyjakso.apply {
                            tyoskentelypaikka?.apply {
                                kunta =
                                    tyoskentelyjaksoDTO.tyoskentelypaikka?.kuntaId?.let { id ->
                                        kuntaRepository.findByIdOrNull(id)
                                    }
                                nimi = tyoskentelyjaksoDTO.tyoskentelypaikka?.nimi
                                tyyppi = updatedTyoskentelyjakso.tyoskentelypaikka?.tyyppi
                                muuTyyppi =
                                    if (tyyppi == MUU) updatedTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi else null
                            }
                            osaaikaprosentti = updatedTyoskentelyjakso.osaaikaprosentti
                            alkamispaiva = updatedTyoskentelyjakso.alkamispaiva
                            paattymispaiva = updatedTyoskentelyjakso.paattymispaiva
                            osaaikaprosentti = updatedTyoskentelyjakso.osaaikaprosentti
                            hyvaksyttyAiempaanErikoisalaan =
                                updatedTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan
                            kaytannonKoulutus = updatedTyoskentelyjakso.kaytannonKoulutus
                            omaaErikoisalaaTukeva =
                                updatedTyoskentelyjakso.omaaErikoisalaaTukeva.takeIf { kaytannonKoulutus == OMAA_ERIKOISALAA_TUKEVA_KOULUTUS }
                                    ?.let {
                                        erikoisalaRepository.findByIdOrNull(it.id)
                                    }
                        }
                    }

                    mapAsiakirjat(
                        tyoskentelyjakso,
                        newAsiakirjat,
                        deletedAsiakirjaIds,
                        tyoskentelyjakso.erikoistuvaLaakari!!
                    )
                }
        }?.let { updated ->
            tyoskentelyjaksoRepository.save(updated)?.let { persisted ->
                return tyoskentelyjaksoMapper.toDto(persisted)
            }
        }

        return null
    }

    private fun mapAsiakirjat(
        tyoskentelyjakso: Tyoskentelyjakso,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?,
        kirjautunutErikoistuvaLaakari: ErikoistuvaLaakari
    ): Tyoskentelyjakso {

        newAsiakirjat.let {
            val asiakirjaEntities = it.map { asiakirjaDTO ->
                asiakirjaDTO.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.id
                asiakirjaDTO.lisattypvm = LocalDateTime.now()

                asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                    this.tyoskentelyjakso = tyoskentelyjakso
                    this.asiakirjaData?.data =
                        BlobProxy.generateProxy(
                            asiakirjaDTO.asiakirjaData?.fileInputStream,
                            asiakirjaDTO.asiakirjaData?.fileSize!!
                        )
                }
            }

            tyoskentelyjakso.asiakirjat.addAll(asiakirjaEntities)
        }

        deletedAsiakirjaIds?.map { x -> x.toLong() }?.let {
            tyoskentelyjakso.asiakirjat.removeIf { asiakirja ->
                asiakirja.id in it
            }
        }

        return tyoskentelyjakso
    }

    private fun isValidTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso): Boolean {

        // Tarkistetaan päättymispäivä suoritusarvioinneille
        tyoskentelyjakso.suoritusarvioinnit.forEach {
            if (it.tapahtumanAjankohta!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                return false
            }
        }

        // Tarkistetaan päättymispäivä suoritemerkinnöille
        tyoskentelyjakso.suoritemerkinnat.forEach {
            if (it.suorituspaiva!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                return false
            }
        }

        // Tarkistetaan päättymispäivä keskeytyksille
        tyoskentelyjakso.keskeytykset.forEach {
            if (it.paattymispaiva!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                return false
            }
        }

        return true
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<TyoskentelyjaksoDTO> {
        log.debug("Request to get list of Tyoskentelyjakso by user id : $userId")

        return tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
            .map(tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO? {
        log.debug("Request to get Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                    ?.let { kirjautunutErikoistuvaLaakari ->
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
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                    ?.let { kirjautunutErikoistuvaLaakari ->
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
        log.debug("Request to get TyoskentelyjaksotTilastot")

        val counter = TilastotCounter()

        val kaytannonKoulutusSuoritettuMap =
            KaytannonKoulutusTyyppi.values().map { it to 0.0 }.toMap().toMutableMap()
        val tyoskentelyjaksotSuoritettu =
            mutableSetOf<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>()
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)

        tyoskentelyjaksot.map {
            getTyoskentelyjaksoTilastot(
                it,
                counter,
                kaytannonKoulutusSuoritettuMap,
                tyoskentelyjaksotSuoritettu
            )
        }

        val erikoisala = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.erikoisala
        val yhteensaVaadittuVahintaan = erikoisala?.kaytannonKoulutuksenVahimmaispituus ?: 0.0
        val arvioErikoistumiseenHyvaksyttavista =
            min(yhteensaVaadittuVahintaan / 2, counter.hyvaksyttyToiselleErikoisalalleSuoritettu) +
                counter.nykyiselleErikoisalalleSuoritettu

        counter.tyoskentelyaikaYhteensa = max(0.0, counter.tyoskentelyaikaYhteensa)

        return TyoskentelyjaksotTilastotDTO(
            tyoskentelyaikaYhteensa = counter.tyoskentelyaikaYhteensa,
            arvioErikoistumiseenHyvaksyttavista = arvioErikoistumiseenHyvaksyttavista,
            arvioPuuttuvastaKoulutuksesta = max(
                0.0,
                yhteensaVaadittuVahintaan - arvioErikoistumiseenHyvaksyttavista
            ),
            koulutustyypit = TyoskentelyjaksotTilastotKoulutustyypitDTO(
                terveyskeskusVaadittuVahintaan = erikoisala?.terveyskeskuskoulutusjaksonVahimmaispituus
                    ?: 0.0,
                terveyskeskusSuoritettu = counter.terveyskeskusSuoritettu,
                yliopistosairaalaVaadittuVahintaan = erikoisala?.yliopistosairaalajaksonVahimmaispituus
                    ?: 0.0,
                yliopistosairaalaSuoritettu = counter.yliopistosairaalaSuoritettu,
                yliopistosairaaloidenUlkopuolinenVaadittuVahintaan =
                erikoisala?.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus ?: 0.0,
                yliopistosairaaloidenUlkopuolinenSuoritettu = counter.yliopistosairaaloidenUlkopuolinenSuoritettu,
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

    override fun updateLiitettyKoejaksoon(
        id: Long,
        userId: String,
        liitettyKoejaksoon: Boolean
    ): TyoskentelyjaksoDTO? {
        tyoskentelyjaksoRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)?.let {
            it.liitettyKoejaksoon = liitettyKoejaksoon
            tyoskentelyjaksoRepository.save(it)
            return tyoskentelyjaksoMapper.toDto(it)
        }

        return null
    }

    fun getTyoskentelyjaksoTilastot(
        tyoskentelyjakso: Tyoskentelyjakso,
        counter: TilastotCounter,
        kaytannonKoulutusSuoritettuMap: MutableMap<KaytannonKoulutusTyyppi, Double>,
        tyoskentelyjaksotSuoritettu: MutableSet<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>
    ) {
        // Lasketaan työskentelyjakson päivät
        val daysBetween = ChronoUnit.DAYS.between(
            tyoskentelyjakso.alkamispaiva,
            tyoskentelyjakso.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        ) + 1

        // Ei huomioida tulevaisuuden jaksoja
        if (daysBetween > 0) {
            val factor = tyoskentelyjakso.osaaikaprosentti!!.toDouble() / 100.0
            var result = factor * daysBetween

            // Vähennetään keskeytykset
            tyoskentelyjakso.keskeytykset.map { keskeytysaika ->
                val keskeytysaikaDaysBetween = ChronoUnit.DAYS.between(
                    keskeytysaika.alkamispaiva,
                    keskeytysaika.paattymispaiva
                ) + 1

                // Keskeytysajan prosentti 0 % tarkoittaa kokopäiväpoissaoloa
                val keskeytysaikaFactor = keskeytysaika.osaaikaprosentti!!.toDouble() / 100.0
                val keskeytysaikaResult = keskeytysaikaFactor * keskeytysaikaDaysBetween

                when (keskeytysaika.poissaolonSyy!!.vahennystyyppi!!) {
                    VAHENNETAAN_YLIMENEVA_AIKA -> {
                        // 30 kalenterivuoden päivän sääntöä ei oteta huomioon vielä. Tarvitaan jokin ratkaisu.
                        result -= keskeytysaikaResult
                    }
                    VAHENNETAAN_SUORAAN -> {
                        result -= keskeytysaikaResult
                    }
                }
            }

            // Koskaan ei summata negatiivisa arvoja laskuriin! (Esim. jos on kirjattu poissaolo useampaan kertaan)
            result = max(0.0, result)

            // Summataan suoritettu aika koulutustyypettäin
            when (tyoskentelyjakso.tyoskentelypaikka!!.tyyppi!!) {
                TERVEYSKESKUS -> counter.terveyskeskusSuoritettu += result
                YLIOPISTOLLINEN_SAIRAALA -> counter.yliopistosairaalaSuoritettu += result
                else -> counter.yliopistosairaaloidenUlkopuolinenSuoritettu += result
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
                counter.hyvaksyttyToiselleErikoisalalleSuoritettu += result
            } else {
                counter.nykyiselleErikoisalalleSuoritettu += result
            }

            // Summataan työskentelyaika yhteensä
            counter.tyoskentelyaikaYhteensa += result

            // Kootaan työskentelyjaksojen suoritetut työskentelyajat
            tyoskentelyjaksotSuoritettu.add(
                TyoskentelyjaksotTilastotTyoskentelyjaksotDTO(
                    id = tyoskentelyjakso.id!!,
                    suoritettu = result
                )
            )
        }
    }

    data class TilastotCounter(
        var terveyskeskusSuoritettu: Double = 0.0,
        var yliopistosairaalaSuoritettu: Double = 0.0,
        var yliopistosairaaloidenUlkopuolinenSuoritettu: Double = 0.0,
        var tyoskentelyaikaYhteensa: Double = 0.0,
        var nykyiselleErikoisalalleSuoritettu: Double = 0.0,
        var hyvaksyttyToiselleErikoisalalleSuoritettu: Double = 0.0
    )
}
