package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi.*
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi.*
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.ceil
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
    private val asiakirjaMapper: AsiakirjaMapper,
    private val tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService
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
            return tyoskentelyjaksoMapper.toDto(updated)
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

    @Transactional(readOnly = true)
    override fun validateByLiitettyKoejaksoon(userId: String): Triple<Boolean, Boolean, Boolean> {
        var tyoskentelyJaksoLiitetty: Boolean
        var tyoskentelyjaksonPituusRiittava: Boolean
        var tyotodistusLiitetty: Boolean

        tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserIdAndLiitettyKoejaksoonTrue(userId).let {
            tyoskentelyJaksoLiitetty = true
            tyoskentelyjaksonPituusRiittava = validateTyoskentelyjaksonPituusKoejaksolleRiittava(it)
            tyotodistusLiitetty = !it.any { tyoskentelyjakso -> tyoskentelyjakso.asiakirjat.isEmpty() }
        }

        return Triple(tyoskentelyJaksoLiitetty, tyoskentelyjaksonPituusRiittava, tyotodistusLiitetty)
    }

    private fun validateTyoskentelyjaksonPituusKoejaksolleRiittava(
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ): Boolean {
        var totalLength = 0.0
        val hyvaksiluettavatCounter = HyvaksiluettavatCounterData().apply {
            hyvaksiluettavatPerYearMap =
                tyoskentelyjaksonPituusCounterService.getHyvaksiluettavatPerYearMap(tyoskentelyjaksot)
        }
        tyoskentelyjaksot.forEach {
            totalLength += tyoskentelyjaksonPituusCounterService.calculateInDays(it, hyvaksiluettavatCounter)
        }

        // Pyöristetään päivät ylöspäin UOELSA-717 mukaisesti
        val years = ceil(totalLength) / 365
        val months = years * 12
        // Koejaksoon liitetyn työskentelyjakson (voi koostua useammasta jaksosta) vähimmäispituus on 6kk.
        return months >= 6
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

        val tilastotCounter = TilastotCounter()
        val kaytannonKoulutusSuoritettuMap =
            KaytannonKoulutusTyyppi.values().map { it to 0.0 }.toMap().toMutableMap()
        val tyoskentelyjaksotSuoritettu =
            mutableSetOf<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>()
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        val hyvaksiluettavatCounter = HyvaksiluettavatCounterData().apply {
            hyvaksiluettavatPerYearMap =
                tyoskentelyjaksonPituusCounterService.getHyvaksiluettavatPerYearMap(tyoskentelyjaksot)
        }

        tyoskentelyjaksot.map {
            getTyoskentelyjaksoTilastot(
                it,
                tilastotCounter,
                hyvaksiluettavatCounter,
                kaytannonKoulutusSuoritettuMap,
                tyoskentelyjaksotSuoritettu
            )
        }

        val erikoisala = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.erikoisala
        val yhteensaVaadittuVahintaan = erikoisala?.kaytannonKoulutuksenVahimmaispituus ?: 0.0
        val arvioErikoistumiseenHyvaksyttavista =
            min(yhteensaVaadittuVahintaan / 2, tilastotCounter.hyvaksyttyToiselleErikoisalalleSuoritettu) +
                tilastotCounter.nykyiselleErikoisalalleSuoritettu

        tilastotCounter.tyoskentelyaikaYhteensa = max(0.0, tilastotCounter.tyoskentelyaikaYhteensa)

        return TyoskentelyjaksotTilastotDTO(
            tyoskentelyaikaYhteensa = tilastotCounter.tyoskentelyaikaYhteensa,
            arvioErikoistumiseenHyvaksyttavista = arvioErikoistumiseenHyvaksyttavista,
            arvioPuuttuvastaKoulutuksesta = max(
                0.0,
                yhteensaVaadittuVahintaan - arvioErikoistumiseenHyvaksyttavista
            ),
            koulutustyypit = TyoskentelyjaksotTilastotKoulutustyypitDTO(
                terveyskeskusVaadittuVahintaan = erikoisala?.terveyskeskuskoulutusjaksonVahimmaispituus
                    ?: 0.0,
                terveyskeskusSuoritettu = tilastotCounter.terveyskeskusSuoritettu,
                yliopistosairaalaVaadittuVahintaan = erikoisala?.yliopistosairaalajaksonVahimmaispituus
                    ?: 0.0,
                yliopistosairaalaSuoritettu = tilastotCounter.yliopistosairaalaSuoritettu,
                yliopistosairaaloidenUlkopuolinenVaadittuVahintaan =
                erikoisala?.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus ?: 0.0,
                yliopistosairaaloidenUlkopuolinenSuoritettu = tilastotCounter.yliopistosairaaloidenUlkopuolinenSuoritettu,
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
        tilastotCounter: TilastotCounter,
        hyvaksiluettavatCounterData: HyvaksiluettavatCounterData,
        kaytannonKoulutusSuoritettuMap: MutableMap<KaytannonKoulutusTyyppi, Double>,
        tyoskentelyjaksotSuoritettu: MutableSet<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>
    ) {
        val tyoskentelyjaksonPituus =
            tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso, hyvaksiluettavatCounterData)
        if (tyoskentelyjaksonPituus > 0) {
            // Summataan suoritettu aika koulutustyypettäin
            when (tyoskentelyjakso.tyoskentelypaikka!!.tyyppi!!) {
                TERVEYSKESKUS -> tilastotCounter.terveyskeskusSuoritettu += tyoskentelyjaksonPituus
                YLIOPISTOLLINEN_SAIRAALA -> tilastotCounter.yliopistosairaalaSuoritettu += tyoskentelyjaksonPituus
                else -> tilastotCounter.yliopistosairaaloidenUlkopuolinenSuoritettu += tyoskentelyjaksonPituus
            }

            // Summataan suoritettu aika käytännön koulutuksettain
            when (tyoskentelyjakso.kaytannonKoulutus!!) {
                OMAN_ERIKOISALAN_KOULUTUS ->
                    kaytannonKoulutusSuoritettuMap[OMAN_ERIKOISALAN_KOULUTUS] =
                        kaytannonKoulutusSuoritettuMap[OMAN_ERIKOISALAN_KOULUTUS]!! + tyoskentelyjaksonPituus
                OMAA_ERIKOISALAA_TUKEVA_KOULUTUS ->
                    kaytannonKoulutusSuoritettuMap[OMAA_ERIKOISALAA_TUKEVA_KOULUTUS] =
                        kaytannonKoulutusSuoritettuMap[OMAA_ERIKOISALAA_TUKEVA_KOULUTUS]!! + tyoskentelyjaksonPituus
                TUTKIMUSTYO ->
                    kaytannonKoulutusSuoritettuMap[TUTKIMUSTYO] =
                        kaytannonKoulutusSuoritettuMap[TUTKIMUSTYO]!! + tyoskentelyjaksonPituus
                TERVEYSKESKUSTYO ->
                    kaytannonKoulutusSuoritettuMap[TERVEYSKESKUSTYO] =
                        kaytannonKoulutusSuoritettuMap[TERVEYSKESKUSTYO]!! + tyoskentelyjaksonPituus
            }

            // Summataan nykyiselle tai hyväksytylle erikoisalalle
            if (tyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan) {
                tilastotCounter.hyvaksyttyToiselleErikoisalalleSuoritettu += tyoskentelyjaksonPituus
            } else {
                tilastotCounter.nykyiselleErikoisalalleSuoritettu += tyoskentelyjaksonPituus
            }

            // Summataan työskentelyaika yhteensä
            tilastotCounter.tyoskentelyaikaYhteensa += tyoskentelyjaksonPituus

            // Kootaan työskentelyjaksojen suoritetut työskentelyajat
            tyoskentelyjaksotSuoritettu.add(
                TyoskentelyjaksotTilastotTyoskentelyjaksotDTO(
                    id = tyoskentelyjakso.id!!,
                    suoritettu = tyoskentelyjaksonPituus
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
