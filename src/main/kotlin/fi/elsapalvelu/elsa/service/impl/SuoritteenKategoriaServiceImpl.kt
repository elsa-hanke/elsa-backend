package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.service.SuoritteenKategoriaService
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.SuoritteenKategoriaWithErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritteenKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritteenKategoriaSimpleMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritteenKategoriaWithErikoisalaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class SuoritteenKategoriaServiceImpl(
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val suoritteenKategoriaMapper: SuoritteenKategoriaMapper,
    private val suoritteenKategoriaSimpleMapper: SuoritteenKategoriaSimpleMapper,
    private val suoritteenKategoriaWithErikoisalaMapper: SuoritteenKategoriaWithErikoisalaMapper
) : SuoritteenKategoriaService {

    override fun save(
        suoritteenKategoriaDTO: SuoritteenKategoriaWithErikoisalaDTO
    ): SuoritteenKategoriaWithErikoisalaDTO {
        var suoritteenKategoria =
            suoritteenKategoriaWithErikoisalaMapper.toEntity(suoritteenKategoriaDTO)
        suoritteenKategoria = suoritteenKategoriaRepository.save(suoritteenKategoria)
        return suoritteenKategoriaWithErikoisalaMapper.toDto(suoritteenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<SuoritteenKategoriaDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(
                it.erikoisala?.id,
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(suoritteenKategoriaMapper::toDto)
        } ?: listOf()
    }

    override fun findAllExpiredByOpintooikeusId(opintooikeusId: Long): List<SuoritteenKategoriaDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            suoritteenKategoriaRepository.findAllByErikoisalaIdAndExpired(
                it.erikoisala?.id,
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(suoritteenKategoriaMapper::toDto)
        } ?: listOf()
    }

    override fun findAllByErikoisalaId(erikoisalaId: Long): List<SuoritteenKategoriaSimpleDTO> {
        return suoritteenKategoriaRepository.findAllByErikoisalaId(erikoisalaId)
            .map(suoritteenKategoriaSimpleMapper::toDto)
    }

    override fun findAllByErikoisalaIdWithKokonaisuudet(erikoisalaId: Long): List<SuoritteenKategoriaDTO> {
        return suoritteenKategoriaRepository.findAllByErikoisalaId(erikoisalaId)
            .map(suoritteenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritteenKategoriaWithErikoisalaDTO> {
        return suoritteenKategoriaRepository.findById(id)
            .map(suoritteenKategoriaWithErikoisalaMapper::toDto)
    }

    override fun delete(id: Long) {
        suoritteenKategoriaRepository.deleteById(id)
    }
}
