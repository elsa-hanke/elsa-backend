package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Opintosuoritus
import fi.elsapalvelu.elsa.domain.OpintosuoritusOsakokonaisuus
import fi.elsapalvelu.elsa.extensions.match
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.OpintosuoritusKurssikoodiRepository
import fi.elsapalvelu.elsa.repository.OpintosuoritusRepository
import fi.elsapalvelu.elsa.service.OpintosuorituksetPersistenceService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusOsakokonaisuusDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusMapper
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusOsakokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusTyyppiMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
@Transactional
class OpintosuorituksetPersistenceServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val opintosuoritusRepository: OpintosuoritusRepository,
    private val opintosuoritusMapper: OpintosuoritusMapper,
    private val opintosuoritusKurssikoodiRepository: OpintosuoritusKurssikoodiRepository,
    private val opintosuoritusOsakokonaisuusMapper: OpintosuoritusOsakokonaisuusMapper,
    private val opintosuoritusTyyppiMapper: OpintosuoritusTyyppiMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : OpintosuorituksetPersistenceService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun createOrUpdateIfChanged(userId: String, opintosuoritukset: OpintosuorituksetPersistenceDTO) {
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val kurssikoodit =
            opintosuoritusKurssikoodiRepository.findAllByYliopistoNimi(opintosuoritukset.yliopisto)

        opintosuoritukset.items?.mapNotNull { opintosuoritusDTO ->
            checkKurssikoodiExistsOrLogError(opintosuoritusDTO, userId)?.let {
                kurssikoodit.find { k ->
                    opintosuoritusDTO.kurssikoodi!!.match(k.tunniste!!)
                }?.tyyppi?.let { tyyppi ->
                    opintosuoritusDTO.tyyppi = opintosuoritusTyyppiMapper.toDto(tyyppi)
                    opintosuoritusDTO
                }
            }
        }?.forEach opintosuoritukset@{ opintosuoritusDTO ->
            checkNimiExistsOrLogError(opintosuoritusDTO, userId) ?: return@opintosuoritukset
            checkSuorituspaivaValidOrLogError(opintosuoritusDTO, userId) ?: return@opintosuoritukset
            checkOpintooikeusIdExistsOrLogError(opintosuoritusDTO, userId) ?: return@opintosuoritukset
            checkArvioExistsOrLogError(opintosuoritusDTO, userId) ?: return@opintosuoritukset

            opintosuoritusDTO.osakokonaisuudet?.forEach { osakokonaisuusDTO ->
                checkKurssikoodiForOsakokonaisuusExistsOrLogError(osakokonaisuusDTO, userId) ?: return@opintosuoritukset
                checkNimiForOsakokonaisuusExistsOrLogError(osakokonaisuusDTO, userId) ?: return@opintosuoritukset
                checkSuorituspaivaForOsakokonaisuusValidOrLogError(osakokonaisuusDTO, userId) ?: return@opintosuoritukset
                checkArvioForOsakokonaisuusExistsOrLogError(osakokonaisuusDTO, userId) ?: return@opintosuoritukset
            }

            opintosuoritusRepository.findOneByOpintooikeusYliopistoOpintooikeusIdAndKurssikoodi(
                opintosuoritusDTO.yliopistoOpintooikeusId!!,
                opintosuoritusDTO.kurssikoodi!!
            )
                ?.let { opintosuoritus ->
                    updateOpintosuoritusDetailsIfChanged(opintosuoritus, opintosuoritusDTO)
                    opintosuoritusDTO.osakokonaisuudet?.forEach { osakokonaisuusDTO ->
                        opintosuoritus.osakokonaisuudet?.find { it.kurssikoodi == osakokonaisuusDTO.kurssikoodi }?.let {
                            updateOpintosuoritusOsakokonaisuusDetailsIfChanged(it, osakokonaisuusDTO)
                        } ?: addNewOpintosuoritusOsakokonaisuus(opintosuoritus, osakokonaisuusDTO)
                    } ?: return
                } ?: addNewOpintosuoritus(opintosuoritusDTO, erikoistuvaLaakari, userId)
        }
    }

    private fun updateOpintosuoritusDetailsIfChanged(
        opintosuoritus: Opintosuoritus,
        opintosuoritusDTO: OpintosuoritusDTO
    ) {
        var updated = false

        opintosuoritusDTO.nimi_fi.takeIf { opintosuoritus.nimi_fi != it }?.let {
            opintosuoritus.nimi_fi = it
            updated = true
        }

        opintosuoritusDTO.nimi_sv.takeIf { opintosuoritus.nimi_sv != it }?.let {
            opintosuoritus.nimi_sv = it
            updated = true
        }

        opintosuoritusDTO.suorituspaiva.takeIf { opintosuoritus.suorituspaiva != it }?.let {
            opintosuoritus.suorituspaiva = it
            updated = true
        }

        opintosuoritusDTO.opintopisteet.takeIf { opintosuoritus.opintopisteet != it }?.let {
            opintosuoritus.opintopisteet = it
            updated = true
        }

        opintosuoritusDTO.hyvaksytty.takeIf { opintosuoritus.hyvaksytty != it }?.let {
            opintosuoritus.hyvaksytty = it
            updated = true
        }

        opintosuoritusDTO.arvio_fi.takeIf { opintosuoritus.arvio_fi != it }?.let {
            opintosuoritus.arvio_fi = it
            updated = true
        }

        opintosuoritusDTO.arvio_sv.takeIf { opintosuoritus.arvio_sv != it }?.let {
            opintosuoritus.arvio_sv = it
            updated = true
        }

        // Päivitetään vanhenemispäivä vaikka se olisi null opintotietojärjestelmästä saadussa datassa,
        // koska vanhenemispäivä saatetaan haluta poistaa joiltakin suorituksilta.
        if (opintosuoritusDTO.vanhenemispaiva != opintosuoritus.vanhenemispaiva) {
            opintosuoritus.vanhenemispaiva = opintosuoritusDTO.vanhenemispaiva
            updated = true
        }

        if (updated) {
            opintosuoritus.muokkausaika = Instant.now()
        }
    }

    private fun updateOpintosuoritusOsakokonaisuusDetailsIfChanged(
        osakokonaisuus: OpintosuoritusOsakokonaisuus,
        osakokonaisuusDTO: OpintosuoritusOsakokonaisuusDTO
    ) {
        var updated = false

        osakokonaisuusDTO.nimi_fi.takeIf { osakokonaisuus.nimi_fi != it }?.let {
            osakokonaisuus.nimi_fi = it
            updated = true
        }

        osakokonaisuusDTO.nimi_sv.takeIf { osakokonaisuus.nimi_sv != it }?.let {
            osakokonaisuus.nimi_sv = it
            updated = true
        }

        osakokonaisuusDTO.suorituspaiva.takeIf { osakokonaisuus.suorituspaiva != it }?.let {
            osakokonaisuus.suorituspaiva = it
            updated = true
        }

        osakokonaisuusDTO.opintopisteet.takeIf { osakokonaisuus.opintopisteet != it }?.let {
            osakokonaisuus.opintopisteet = it
            updated = true
        }

        osakokonaisuusDTO.hyvaksytty.takeIf { osakokonaisuus.hyvaksytty != it }?.let {
            osakokonaisuus.hyvaksytty = it
            updated = true
        }

        osakokonaisuusDTO.arvio_fi.takeIf { osakokonaisuus.arvio_fi != it }?.let {
            osakokonaisuus.arvio_fi = it
            updated = true
        }

        osakokonaisuusDTO.arvio_sv.takeIf { osakokonaisuus.arvio_sv != it }?.let {
            osakokonaisuus.arvio_sv = it
            updated = true
        }

        // Päivitetään vanhenemispäivä vaikka se olisi null opintotietojärjestelmästä saadussa datassa,
        // koska vanhenemispäivä saatetaan haluta poistaa joiltakin osakokonaisuuksilta.
        if (osakokonaisuusDTO.vanhenemispaiva != osakokonaisuus.vanhenemispaiva) {
            osakokonaisuus.vanhenemispaiva = osakokonaisuusDTO.vanhenemispaiva
            updated = true
        }

        if (updated) {
            osakokonaisuus.muokkausaika = Instant.now()
        }
    }

    private fun addNewOpintosuoritus(
        opintosuoritusDTO: OpintosuoritusDTO,
        erikoistuvaLaakari: ErikoistuvaLaakari?,
        userId: String
    ) {
        val opintooikeus = findOpintooikeusOrLogError(
            opintosuoritusDTO,
            erikoistuvaLaakari?.id!!,
            userId
        ) ?: return

        val opintosuoritus = opintosuoritusMapper.toEntity(opintosuoritusDTO).apply {
            this.opintooikeus = opintooikeus
            muokkausaika = Instant.now()
        }
        opintosuoritusRepository.save(opintosuoritus)

        opintosuoritusDTO.osakokonaisuudet?.map {
            opintosuoritusOsakokonaisuusMapper.toEntity(it).apply {
                this.opintosuoritus = opintosuoritus
                muokkausaika = Instant.now()
            }
        }?.let {
            opintosuoritus.osakokonaisuudet?.addAll(it)
        }
    }

    private fun addNewOpintosuoritusOsakokonaisuus(
        opintosuoritus: Opintosuoritus,
        osakokonaisuusDTO: OpintosuoritusOsakokonaisuusDTO
    ) {
        val osakokonaisuus = opintosuoritusOsakokonaisuusMapper.toEntity(osakokonaisuusDTO).apply {
            this.opintosuoritus = opintosuoritus
            muokkausaika = Instant.now()
        }
        opintosuoritus.osakokonaisuudet?.add(osakokonaisuus)
    }

    private fun checkKurssikoodiExistsOrLogError(opintosuoritusDTO: OpintosuoritusDTO, userId: String): String? =
        opintosuoritusDTO.kurssikoodi ?: run {
            log.error(
                "${javaClass.name}: user id: $userId. Kurssikoodia ei ole asetettu" +
                    " opintosuoritukselle $opintosuoritusDTO"
            )
            return null
        }

    private fun checkKurssikoodiForOsakokonaisuusExistsOrLogError(
        osakokonaisuusDTO: OpintosuoritusOsakokonaisuusDTO,
        userId: String
    ): String? =
        osakokonaisuusDTO.kurssikoodi ?: run {
            log.error(
                "${javaClass.name}: user id: $userId. Kurssikoodia ei ole asetettu" +
                    " opintosuorituksen osakokonaisuudelle $osakokonaisuusDTO"
            )
            return null
        }

    // Vähintään suomenkielinen nimi opintosuoritukselle täytyy löytyä.
    private fun checkNimiExistsOrLogError(
        opintosuoritusDTO: OpintosuoritusDTO,
        userId: String
    ): String? = opintosuoritusDTO.nimi_fi ?: run {
        log.error(
            "${javaClass.name}: user id: $userId. Nimeä (fi) ei ole asetettu" +
                " opintosuoritukselle $opintosuoritusDTO"
        )
        return null
    }

    private fun checkSuorituspaivaValidOrLogError(
        opintosuoritusDTO: OpintosuoritusDTO,
        userId: String
    ): LocalDate? = opintosuoritusDTO.suorituspaiva ?: run {
        log.error(
            "${javaClass.name}: user id: $userId. Kelvollista suorituspäivämäärää ei ole asetettu" +
                " opintosuoritukselle $opintosuoritusDTO"
        )
        return null
    }


    // Vähintään suomenkielinen nimi osakokonaisuudelle täytyy löytyä.
    private fun checkNimiForOsakokonaisuusExistsOrLogError(
        osakokonaisuusDTO: OpintosuoritusOsakokonaisuusDTO,
        userId: String,
    ): String? = osakokonaisuusDTO.nimi_fi ?: run {
        log.error(
            "${javaClass.name}: user id: $userId. Nimeä (fi) ei ole asetettu opintosuorituksen" +
                " osakokonaisuudelle $osakokonaisuusDTO"
        )
        return null
    }

    private fun checkSuorituspaivaForOsakokonaisuusValidOrLogError(
        osakokonaisuusDTO: OpintosuoritusOsakokonaisuusDTO,
        userId: String
    ): LocalDate? = osakokonaisuusDTO.suorituspaiva ?: run {
        log.error(
            "${javaClass.name}: user id: $userId. Kelvollista suorituspäivämäärää ei ole asetettu" +
                " opintosuorituksen osakokonaisuudelle $osakokonaisuusDTO"
        )
        return null
    }

    private fun checkOpintooikeusIdExistsOrLogError(
        opintosuoritusDTO: OpintosuoritusDTO,
        userId: String
    ): String? =
        opintosuoritusDTO.yliopistoOpintooikeusId ?: run {
            log.error(
                "${javaClass.name}: user id: $userId. Opinto-oikeus id:tä ei ole asetettu" +
                    " opintosuoritukselle $opintosuoritusDTO."
            )
            return null
        }

    private fun checkArvioExistsOrLogError(
        opintosuoritusDTO: OpintosuoritusDTO,
        userId: String
    ): Boolean? =
        opintosuoritusDTO.hyvaksytty ?: run {
            log.error(
                "${javaClass.name}: user id: $userId. Arviota (hyväksytty/hylätty) ei ole asetettu " +
                    "opintosuoritukselle $opintosuoritusDTO."
            )
            return null
        }

    private fun checkArvioForOsakokonaisuusExistsOrLogError(
        osakokonaisuusDTO: OpintosuoritusOsakokonaisuusDTO,
        userId: String
    ): Boolean? =
        osakokonaisuusDTO.hyvaksytty ?: run {
            log.error(
                "${javaClass.name}: user id: $userId. Arviota (hyväksytty/hylätty) ei ole asetettu opintosuorituksen" +
                    " osakokonaisuudelle $osakokonaisuusDTO"
            )
            return null
        }

    private fun findOpintooikeusOrLogError(
        opintosuoritusDTO: OpintosuoritusDTO,
        erikoistuvaLaakariId: Long,
        userId: String
    ): Opintooikeus? =
        opintooikeusRepository.findOneByErikoistuvaLaakariIdAndYliopistoOpintooikeusId(
            erikoistuvaLaakariId,
            opintosuoritusDTO.yliopistoOpintooikeusId!!
        ) ?: run {
            log.error(
                "${javaClass.name}: user id: $userId. Opinto-oikeutta ei löydy Elsasta" +
                    " opintosuoritukselle $opintosuoritusDTO."
            )
            return null
        }
}
