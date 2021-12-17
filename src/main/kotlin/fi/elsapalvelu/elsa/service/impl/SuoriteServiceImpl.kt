package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoriteRepository
import fi.elsapalvelu.elsa.service.SuoriteService
import fi.elsapalvelu.elsa.service.dto.SuoriteDTO
import fi.elsapalvelu.elsa.service.mapper.SuoriteMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class SuoriteServiceImpl(
    private val suoriteRepository: SuoriteRepository,
    private val suoriteMapper: SuoriteMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : SuoriteService {

    override fun save(
        suoriteDTO: SuoriteDTO
    ): SuoriteDTO {
        var suorite = suoriteMapper.toEntity(suoriteDTO)
        suorite = suoriteRepository.save(suorite)
        return suoriteMapper.toDto(suorite)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<SuoriteDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val opintooikeus = kirjautunutErikoistuvaLaakari?.opintooikeudet?.firstOrNull()

        // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä ei ole määritetty, käytetään nykyistä päivää
        // voimassaolon rajaamisessa
        return suoriteRepository.findAllByValid(
            opintooikeus?.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
        )
            .map(suoriteMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<SuoriteDTO> {
        return suoriteRepository.findById(id)
            .map(suoriteMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        suoriteRepository.deleteById(id)
    }
}
