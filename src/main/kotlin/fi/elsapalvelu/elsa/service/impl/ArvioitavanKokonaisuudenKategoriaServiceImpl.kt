package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.ArvioitavanKokonaisuudenKategoriaService
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaSimpleMapper
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class ArvioitavanKokonaisuudenKategoriaServiceImpl(
    private val arvioitavanKokonaisuudenKategoriaRepository: ArvioitavanKokonaisuudenKategoriaRepository,
    private val arvioitavanKokonaisuudenKategoriaMapper: ArvioitavanKokonaisuudenKategoriaMapper,
    private val arvioitavanKokonaisuudenKategoriaSimpleMapper: ArvioitavanKokonaisuudenKategoriaSimpleMapper,
    private val arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper: ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : ArvioitavanKokonaisuudenKategoriaService {

    override fun save(
        arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
    ): ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO {
        var arvioitavanKokonaisuudenKategoria =
            arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper.toEntity(
                arvioitavanKokonaisuudenKategoriaDTO
            )
        arvioitavanKokonaisuudenKategoria =
            arvioitavanKokonaisuudenKategoriaRepository.save(arvioitavanKokonaisuudenKategoria)
        return arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper.toDto(
            arvioitavanKokonaisuudenKategoria
        )
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        return arvioitavanKokonaisuudenKategoriaRepository.findAll()
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(
                it.erikoisala?.id,
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
        } ?: listOf()
    }

    override fun findAllByErikoisalaId(erikoisalaId: Long): List<ArvioitavanKokonaisuudenKategoriaSimpleDTO> {
        return arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaId(erikoisalaId)
            .map(arvioitavanKokonaisuudenKategoriaSimpleMapper::toDto)
    }

    override fun findAllByErikoisalaIdWithKokonaisuudet(erikoisalaId: Long): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        return arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaId(erikoisalaId)
            .map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO> {
        return arvioitavanKokonaisuudenKategoriaRepository.findById(id)
            .map(arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper::toDto)
    }

    override fun delete(id: Long) {
        arvioitavanKokonaisuudenKategoriaRepository.deleteById(id)
    }
}
