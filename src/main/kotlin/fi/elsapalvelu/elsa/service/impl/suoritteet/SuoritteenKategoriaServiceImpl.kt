package fi.elsapalvelu.elsa.service.impl.suoritteet

import java.time.LocalDate
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.service.suoritteet.SuoritteenKategoriaService
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaWithErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritteenKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritteenKategoriaSimpleMapper
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritteenKategoriaWithErikoisalaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
        }.orEmpty()
    }

    override fun findAllExpiredByOpintooikeusId(opintooikeusId: Long): List<SuoritteenKategoriaDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            suoritteenKategoriaRepository.findAllByErikoisalaIdAndExpired(
                it.erikoisala?.id,
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(suoritteenKategoriaMapper::toDto)
        }.orEmpty()
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
