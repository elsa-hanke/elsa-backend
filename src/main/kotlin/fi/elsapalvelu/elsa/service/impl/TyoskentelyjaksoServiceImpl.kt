package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi.*
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi.*
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Service
@Transactional
class TyoskentelyjaksoServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val kuntaRepository: KuntaRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper,
    private val asiakirjaMapper: AsiakirjaMapper,
    private val tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService,
    private val opintooikeusRepository: OpintooikeusRepository

) : TyoskentelyjaksoService {

    override fun create(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        opintooikeusId: Long,
        newAsiakirjat: MutableSet<AsiakirjaDTO>
    ): TyoskentelyjaksoDTO? {
        opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO).apply {
                opintooikeus = it
                tyoskentelypaikka?.kunta =
                    kuntaRepository.findByIdOrNull(tyoskentelyjaksoDTO.tyoskentelypaikka!!.kuntaId)
                omaaErikoisalaaTukeva =
                    tyoskentelyjaksoDTO.omaaErikoisalaaTukeva.takeIf {
                        kaytannonKoulutus == OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
                    }?.let {
                        if (it.id == null) null else erikoisalaRepository.findByIdOrNull(it.id)
                    }
            }.takeIf { isValidTyoskentelyjakso(it) }?.let { tyoskentelyjakso ->
                mapAsiakirjat(
                    tyoskentelyjakso,
                    newAsiakirjat,
                    null,
                    it
                )
            }?.let {
                tyoskentelyjaksoRepository.save(it).let { saved ->
                    return tyoskentelyjaksoMapper.toDto(saved)
                }
            }
        }

        return null
    }

    override fun update(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        opintooikeusId: Long,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?
    ): TyoskentelyjaksoDTO? {
        tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(
            tyoskentelyjaksoDTO.id!!,
            opintooikeusId
        )?.let { tyoskentelyjakso ->
            // Jos työskentelyjaksolle on lisätty arviointeja tai arviointipyyntöjä, sallitaan vain
            // päättymispäivän muokkaus.
            tyoskentelyjakso.takeIf { it.hasTapahtumia() }
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
                tyoskentelyjakso.opintooikeus!!
            )
        }?.let { updated ->
            return tyoskentelyjaksoMapper.toDto(updated)
        }

        return null
    }

    private fun mapAsiakirjat(
        tyoskentelyjakso: Tyoskentelyjakso,
        newAsiakirjat: MutableSet<AsiakirjaDTO>,
        deletedAsiakirjaIds: MutableSet<Int>?,
        opintooikeus: Opintooikeus
    ): Tyoskentelyjakso {
        newAsiakirjat.let {
            val asiakirjaEntities = it.map { asiakirjaDTO ->
                asiakirjaMapper.toEntity(asiakirjaDTO).apply {
                    this.lisattypvm = LocalDateTime.now()
                    this.opintooikeus = opintooikeus
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
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<TyoskentelyjaksoDTO> {
        return tyoskentelyjaksoRepository.findAllByOpintooikeusId(opintooikeusId)
            .map(tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, opintooikeusId: Long): TyoskentelyjaksoDTO? {
        tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)?.let {
            return tyoskentelyjaksoMapper.toDto(it)
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun validateByLiitettyKoejaksoon(opintooikeusId: Long): Triple<Boolean, Boolean, Boolean> {
        val tyoskentelyjaksotLiitetty =
            tyoskentelyjaksoRepository.findAllByOpintooikeusIdAndLiitettyKoejaksoonTrue(
                opintooikeusId
            )
        val tyoskentelyJaksoLiitetty = tyoskentelyjaksotLiitetty.any()
        val tyoskentelyjaksonPituusRiittava =
            validateTyoskentelyjaksonPituusKoejaksolleRiittava(tyoskentelyjaksotLiitetty)
        val tyotodistusLiitetty =
            !tyoskentelyjaksotLiitetty.any { tyoskentelyjakso -> tyoskentelyjakso.asiakirjat.isEmpty() }

        return Triple(
            tyoskentelyJaksoLiitetty,
            tyoskentelyjaksonPituusRiittava,
            tyotodistusLiitetty
        )
    }

    private fun validateTyoskentelyjaksonPituusKoejaksolleRiittava(
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ): Boolean {
        var totalLength = 0.0
        val hyvaksiluettavatCounter = HyvaksiluettavatCounterData().apply {
            hyvaksiluettavatPerYearMap =
                tyoskentelyjaksonPituusCounterService.getHyvaksiluettavatPerYearMap(
                    tyoskentelyjaksot
                )
        }
        tyoskentelyjaksot.forEach {
            totalLength += tyoskentelyjaksonPituusCounterService.calculateInDays(
                it,
                hyvaksiluettavatCounter
            )
        }

        // Pyöristetään päivät ylöspäin UOELSA-717 mukaisesti
        val years = round(totalLength) / 365
        val months = years * 12
        // Koejaksoon liitetyn työskentelyjakson (voi koostua useammasta jaksosta) vähimmäispituus on 6kk.
        return months >= 6
    }

    override fun delete(id: Long, opintooikeusId: Long): Boolean {
        tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)?.let {
            if (!it.hasTapahtumia()) {
                tyoskentelyjaksoRepository.deleteById(it.id!!)
                return true
            }
        }
        return false
    }

    @Transactional(readOnly = true)
    override fun getTilastot(opintooikeusId: Long): TyoskentelyjaksotTilastotDTO {
        val tilastotCounter = TilastotCounter()
        val kaytannonKoulutusSuoritettuMap =
            KaytannonKoulutusTyyppi.values().map { it to 0.0 }.toMap().toMutableMap()
        val tyoskentelyjaksotSuoritettu =
            mutableSetOf<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO>()
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByOpintooikeusId(opintooikeusId)
        val hyvaksiluettavatCounter = HyvaksiluettavatCounterData().apply {
            hyvaksiluettavatPerYearMap =
                tyoskentelyjaksonPituusCounterService.getHyvaksiluettavatPerYearMap(
                    tyoskentelyjaksot
                )
        }
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId).get()

        tyoskentelyjaksot.map {
            getTyoskentelyjaksoTilastot(
                it,
                tilastotCounter,
                hyvaksiluettavatCounter,
                kaytannonKoulutusSuoritettuMap,
                tyoskentelyjaksotSuoritettu
            )
        }

        val yhteensaVaadittuVahintaan =
            opintooikeus.opintoopas?.kaytannonKoulutuksenVahimmaispituus ?: 0.0
        val arvioErikoistumiseenHyvaksyttavista =
            min(
                yhteensaVaadittuVahintaan / 2,
                tilastotCounter.hyvaksyttyToiselleErikoisalalleSuoritettu
            ) +
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
                terveyskeskusVaadittuVahintaan = opintooikeus.opintoopas?.terveyskeskuskoulutusjaksonVahimmaispituus
                    ?: 0.0,
                terveyskeskusSuoritettu = tilastotCounter.terveyskeskusSuoritettu,
                yliopistosairaalaVaadittuVahintaan = opintooikeus.opintoopas?.yliopistosairaalajaksonVahimmaispituus
                    ?: 0.0,
                yliopistosairaalaSuoritettu = tilastotCounter.yliopistosairaalaSuoritettu,
                yliopistosairaaloidenUlkopuolinenVaadittuVahintaan =
                opintooikeus.opintoopas?.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus
                    ?: 0.0,
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
        opintooikeusId: Long,
        liitettyKoejaksoon: Boolean
    ): TyoskentelyjaksoDTO? {
        tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)?.let {
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
            tyoskentelyjaksonPituusCounterService.calculateInDays(
                tyoskentelyjakso,
                hyvaksiluettavatCounterData
            )
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

    override fun validatePaattymispaiva(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        opintooikeusId: Long
    ): Boolean {
        return tyoskentelyjaksoDTO.id?.let { tyoskentelyjaksoId ->
            tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(
                tyoskentelyjaksoId,
                opintooikeusId
            )
        }?.let { tyoskentelyjakso ->
            val paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva ?: return true

            for (suoritemerkinta in tyoskentelyjakso.suoritemerkinnat) {
                if (paattymispaiva.isBefore(suoritemerkinta.suorituspaiva)) {
                    return false
                }
            }

            for (keskeytys in tyoskentelyjakso.keskeytykset) {
                if (paattymispaiva.isBefore(keskeytys.paattymispaiva)) {
                    return false
                }
            }

            for (suoritusarviointi in tyoskentelyjakso.suoritusarvioinnit) {
                if (paattymispaiva.isBefore(suoritusarviointi.tapahtumanAjankohta)) {
                    return false
                }
            }

            true
        } ?: true
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
