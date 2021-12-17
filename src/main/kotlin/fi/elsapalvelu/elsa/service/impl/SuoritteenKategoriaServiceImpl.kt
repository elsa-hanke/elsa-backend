package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.service.SuoritteenKategoriaService
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritteenKategoriaMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class SuoritteenKategoriaServiceImpl(
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository,
    private val suoritteenKategoriaMapper: SuoritteenKategoriaMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : SuoritteenKategoriaService {

    override fun save(
        suoritteenKategoriaDTO: SuoritteenKategoriaDTO
    ): SuoritteenKategoriaDTO {
        var suoritteenKategoria = suoritteenKategoriaMapper.toEntity(suoritteenKategoriaDTO)
        suoritteenKategoria = suoritteenKategoriaRepository.save(suoritteenKategoria)
        return suoritteenKategoriaMapper.toDto(suoritteenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<SuoritteenKategoriaDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val opintooikeus = kirjautunutErikoistuvaLaakari?.opintooikeudet?.firstOrNull()

        return suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(
            opintooikeus?.erikoisala?.id,
            opintooikeus?.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
        )
            .map(suoritteenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<SuoritteenKategoriaDTO> {
        return suoritteenKategoriaRepository.findById(id)
            .map(suoritteenKategoriaMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        suoritteenKategoriaRepository.deleteById(id)
    }
}
