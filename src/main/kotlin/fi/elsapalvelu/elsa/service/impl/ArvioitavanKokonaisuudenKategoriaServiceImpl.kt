package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ArvioitavanKokonaisuudenKategoriaService
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class ArvioitavanKokonaisuudenKategoriaServiceImpl(
    private val arvioitavanKokonaisuudenKategoriaRepository: ArvioitavanKokonaisuudenKategoriaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val arvioitavanKokonaisuudenKategoriaMapper: ArvioitavanKokonaisuudenKategoriaMapper,
) : ArvioitavanKokonaisuudenKategoriaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(
        arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaDTO
    ): ArvioitavanKokonaisuudenKategoriaDTO {
        log.debug("Request to save ArvioitavanKokonaisuudenKategoria : $arvioitavanKokonaisuudenKategoriaDTO")

        var arvioitavanKokonaisuudenKategoria =
            arvioitavanKokonaisuudenKategoriaMapper.toEntity(arvioitavanKokonaisuudenKategoriaDTO)
        arvioitavanKokonaisuudenKategoria =
            arvioitavanKokonaisuudenKategoriaRepository.save(arvioitavanKokonaisuudenKategoria)
        return arvioitavanKokonaisuudenKategoriaMapper.toDto(arvioitavanKokonaisuudenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        log.debug("Request to get all ArvioitavanKokonaisuudenKategoriat")

        return arvioitavanKokonaisuudenKategoriaRepository.findAll()
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        return arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(
            kirjautunutErikoistuvaLaakari?.erikoisala?.id,
            kirjautunutErikoistuvaLaakari?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now()
        )
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<ArvioitavanKokonaisuudenKategoriaDTO> {
        log.debug("Request to get ArvioitavanKokonaisuudenKategoria : $id")

        return arvioitavanKokonaisuudenKategoriaRepository.findById(id)
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        log.debug("Request to delete ArvioitavanKokonaisuudenKategoria : $id")

        arvioitavanKokonaisuudenKategoriaRepository.deleteById(id)
    }
}
