package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.ArvioitavanKokonaisuudenKategoriaService
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaMapper
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
    private val opintooikeusRepository: OpintooikeusRepository
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
    override fun findAllByOpintooikeusId(
        opintooikeusId: Long
    ): List<ArvioitavanKokonaisuudenKategoriaDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(
                it.erikoisala?.id,
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(arvioitavanKokonaisuudenKategoriaMapper::toDto)
        } ?: listOf()
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
