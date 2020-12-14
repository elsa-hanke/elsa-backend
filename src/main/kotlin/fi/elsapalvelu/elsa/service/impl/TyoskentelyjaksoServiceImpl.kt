package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotKaytannonKoulutusDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksotTilastotKoulutustyypitDTO
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
        var terveyskeskusSuoritettu = 0.0
        val yhteensaSuoritettu = 365.0

        val tyoskentelyjaksot = tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        tyoskentelyjaksot.map { tyoskentelyjakso ->
            if (tyoskentelyjakso.tyoskentelypaikka!!.tyyppi!! == TyoskentelyjaksoTyyppi.TERVEYSKESKUS) {
                val daysBetween = ChronoUnit.DAYS.between(
                    tyoskentelyjakso.alkamispaiva,
                    tyoskentelyjakso.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
                ) + 1
                if (daysBetween > 0) {
                    val factor = (tyoskentelyjakso.osaaikaprosentti!!.toDouble() / 100.0)
                    terveyskeskusSuoritettu += factor * daysBetween
                }
            }
        }
        val tyoskentelyaikaYhteensa = terveyskeskusSuoritettu
        val arvioErikoistumiseenHyvaksyttavista = yhteensaSuoritettu
        return TyoskentelyjaksotTilastotDTO(
            tyoskentelyaikaYhteensa = tyoskentelyaikaYhteensa,
            arvioErikoistumiseenHyvaksyttavista = arvioErikoistumiseenHyvaksyttavista,
            arvioPuuttuvastaKoulutuksesta = 740.70,
            koulutustyypit = TyoskentelyjaksotTilastotKoulutustyypitDTO(
                terveyskeskusVaadittuVahintaan = 273.75,
                terveyskeskusSuoritettu = terveyskeskusSuoritettu,
                yliopistosairaalaVaadittuVahintaan = 365.0,
                yliopistosairaalaSuoritettu = 0.0,
                yliopistosairaaloidenUlkopuolinenVaadittuVahintaan = 365.0,
                yliopistosairaaloidenUlkopuolinenSuoritettu = 0.0,
                yhteensaVaadittuVahintaan = 1825.0,
                yhteensaSuoritettu = yhteensaSuoritettu
            ),
            kaytannonKoulutus = KaytannonKoulutusTyyppi.values().map {
                TyoskentelyjaksotTilastotKaytannonKoulutusDTO(
                    kaytannonKoulutus = it,
                    suoritettu = 0.0
                )
            }
                .toMutableSet()
        )
    }
}
