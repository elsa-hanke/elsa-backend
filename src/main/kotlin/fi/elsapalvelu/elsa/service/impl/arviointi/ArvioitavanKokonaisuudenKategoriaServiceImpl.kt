package fi.elsapalvelu.elsa.service.impl.arviointi

import java.time.LocalDate
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.arviointi.ArvioitavanKokonaisuudenKategoriaService
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaSimpleDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavanKokonaisuudenKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavanKokonaisuudenKategoriaSimpleMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
        }.orEmpty()
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
