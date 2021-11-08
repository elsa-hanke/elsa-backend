package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ArvioitavanKokonaisuudenKategoriaService
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaMapper
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

    override fun save(
        arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaDTO
    ): ArvioitavanKokonaisuudenKategoriaDTO {
        var arvioitavanKokonaisuudenKategoria =
            arvioitavanKokonaisuudenKategoriaMapper.toEntity(arvioitavanKokonaisuudenKategoriaDTO)
        arvioitavanKokonaisuudenKategoria =
            arvioitavanKokonaisuudenKategoriaRepository.save(arvioitavanKokonaisuudenKategoria)
        return arvioitavanKokonaisuudenKategoriaMapper.toDto(arvioitavanKokonaisuudenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        return arvioitavanKokonaisuudenKategoriaRepository.findAll()
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val opiskeluoikeus = kirjautunutErikoistuvaLaakari?.opiskeluoikeudet?.firstOrNull()

        return arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(
            opiskeluoikeus?.erikoisala?.id,
            opiskeluoikeus?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now()
        )
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<ArvioitavanKokonaisuudenKategoriaDTO> {
        return arvioitavanKokonaisuudenKategoriaRepository.findById(id)
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        arvioitavanKokonaisuudenKategoriaRepository.deleteById(id)
    }
}
