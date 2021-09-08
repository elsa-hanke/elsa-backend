package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OppimistavoiteRepository
import fi.elsapalvelu.elsa.service.OppimistavoiteService
import fi.elsapalvelu.elsa.service.dto.OppimistavoiteDTO
import fi.elsapalvelu.elsa.service.mapper.OppimistavoiteMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class OppimistavoiteServiceImpl(
    private val oppimistavoiteRepository: OppimistavoiteRepository,
    private val oppimistavoiteMapper: OppimistavoiteMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : OppimistavoiteService {

    override fun save(
        oppimistavoiteDTO: OppimistavoiteDTO
    ): OppimistavoiteDTO {
        var oppimistavoite = oppimistavoiteMapper.toEntity(oppimistavoiteDTO)
        oppimistavoite = oppimistavoiteRepository.save(oppimistavoite)
        return oppimistavoiteMapper.toDto(oppimistavoite)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<OppimistavoiteDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä ei ole määritetty, käytetään nykyistä päivää
        // voimassaolon rajaamisessa
        return oppimistavoiteRepository.findAllByValid(
            kirjautunutErikoistuvaLaakari?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now())
            .map(oppimistavoiteMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<OppimistavoiteDTO> {
        return oppimistavoiteRepository.findById(id)
            .map(oppimistavoiteMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        oppimistavoiteRepository.deleteById(id)
    }
}
