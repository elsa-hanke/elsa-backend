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
        kayttajaId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>
    ): TyoskentelyjaksoDTO? {
        val resultTyoskentelyjakso = erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
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
            }

        if (resultTyoskentelyjakso != null) {
            tyoskentelyjaksoRepository.save(resultTyoskentelyjakso).let { saved ->
                return tyoskentelyjaksoMapper.toDto(saved)
            }
        }

        return null
    }

    override fun update(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        kayttajaId: String,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): TyoskentelyjaksoDTO? {
        val resultTyoskentelyjakso = tyoskentelyjaksoDTO.id?.let { id ->
            tyoskentelyjaksoRepository.findOneByIdAndErikoistuvaLaakariKayttajaId(id, kayttajaId)
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
        }

        if (resultTyoskentelyjakso != null) {
            tyoskentelyjaksoRepository.save(resultTyoskentelyjakso).let { persisted ->
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
    override fun findAllByErikoistuvaLaakariKayttajaId(kayttajaId: String): List<TyoskentelyjaksoDTO> {
        log.debug("Request to get list of Tyoskentelyjakso by user id : $kayttajaId")

        return tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaId(kayttajaId)
            .map(tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, kayttajaId: String): TyoskentelyjaksoDTO? {
        log.debug("Request to get Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
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
    override fun validateByLiitettyKoejaksoon(kayttajaId: String): Triple<Boolean, Boolean, Boolean> {
        var tyoskentelyJaksoLiitetty = false
        var tyoskentelyjaksonPituusRiittava = false
        var tyotodistusLiitetty = false

        tyoskentelyjaksoRepository.findOneByErikoistuvaLaakariKayttajaIdAndLiitettyKoejaksoonTrue(kayttajaId)?.let {
            tyoskentelyJaksoLiitetty = true
            tyoskentelyjaksonPituusRiittava = validateTyoskentelyjaksonPituusKoejaksolleRiittava(it)
            tyotodistusLiitetty = it.asiakirjat.isNotEmpty()
        }

        return Triple(tyoskentelyJaksoLiitetty, tyoskentelyjaksonPituusRiittava, tyotodistusLiitetty)
    }

    private fun validateTyoskentelyjaksonPituusKoejaksolleRiittava(tyoskentelyjakso: Tyoskentelyjakso): Boolean {
        val tyoskentelyJaksonPituusDays = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)
        val years = tyoskentelyJaksonPituusDays / 365
        val months = years * 12
        // Koejaksoon liitetyn työskentelyjakson vähimmäispituus on 6kk.
        return months >= 6
    }

    override fun delete(id: Long, kayttajaId: String) {
        log.debug("Request to delete Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
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
    override fun getTilastot(kayttajaId: String): TyoskentelyjaksotTilastotDTO {
        log.debug("Request to get TyoskentelyjaksotTilastot")

        val counter = TilastotCounter()

        val kaytannonKoulutusSuoritettuMap =
            KaytannonKoulutusTyyppi.values().map { it to 0.0 }.toMap().toMutableMap()
        val tyoskentelyjaksotSuoritettu =
            mutableSetOf<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>()
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaId(kayttajaId)

        tyoskentelyjaksot.map {
            getTyoskentelyjaksoTilastot(
                it,
                counter,
                kaytannonKoulutusSuoritettuMap,
                tyoskentelyjaksotSuoritettu
            )
        }

        val erikoisala = erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)?.erikoisala
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
        kayttajaId: String,
        liitettyKoejaksoon: Boolean
    ): TyoskentelyjaksoDTO? {
        tyoskentelyjaksoRepository.findOneByIdAndErikoistuvaLaakariKayttajaId(id, kayttajaId)?.let {
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
        val tyoskentelyjaksonPituus = tyoskentelyjaksonPituusCounterService.calculateInDays(tyoskentelyjakso)
        if (tyoskentelyjaksonPituus > 0) {
            // Summataan suoritettu aika koulutustyypettäin
            when (tyoskentelyjakso.tyoskentelypaikka!!.tyyppi!!) {
                TERVEYSKESKUS -> counter.terveyskeskusSuoritettu += tyoskentelyjaksonPituus
                YLIOPISTOLLINEN_SAIRAALA -> counter.yliopistosairaalaSuoritettu += tyoskentelyjaksonPituus
                else -> counter.yliopistosairaaloidenUlkopuolinenSuoritettu += tyoskentelyjaksonPituus
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
                counter.hyvaksyttyToiselleErikoisalalleSuoritettu += tyoskentelyjaksonPituus
            } else {
                counter.nykyiselleErikoisalalleSuoritettu += tyoskentelyjaksonPituus
            }

            // Summataan työskentelyaika yhteensä
            counter.tyoskentelyaikaYhteensa += tyoskentelyjaksonPituus

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
