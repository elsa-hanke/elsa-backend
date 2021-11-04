package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OppimistavoitteenKategoriaRepository
import fi.elsapalvelu.elsa.service.OppimistavoitteenKategoriaService
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.OppimistavoitteenKategoriaMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class OppimistavoitteenKategoriaServiceImpl(
    private val oppimistavoitteenKategoriaRepository: OppimistavoitteenKategoriaRepository,
    private val oppimistavoitteenKategoriaMapper: OppimistavoitteenKategoriaMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : OppimistavoitteenKategoriaService {

    override fun save(
        oppimistavoitteenKategoriaDTO: OppimistavoitteenKategoriaDTO
    ): OppimistavoitteenKategoriaDTO {
        var oppimistavoitteenKategoria = oppimistavoitteenKategoriaMapper.toEntity(oppimistavoitteenKategoriaDTO)
        oppimistavoitteenKategoria = oppimistavoitteenKategoriaRepository.save(oppimistavoitteenKategoria)
        return oppimistavoitteenKategoriaMapper.toDto(oppimistavoitteenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<OppimistavoitteenKategoriaDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val opiskeluoikeus = kirjautunutErikoistuvaLaakari?.opiskeluoikeudet?.first()

        return oppimistavoitteenKategoriaRepository.findAllByErikoisalaIdAndValid(
            opiskeluoikeus?.erikoisala?.id,
            opiskeluoikeus?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now()
        )
            .map(oppimistavoitteenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<OppimistavoitteenKategoriaDTO> {
        return oppimistavoitteenKategoriaRepository.findById(id)
            .map(oppimistavoitteenKategoriaMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        oppimistavoitteenKategoriaRepository.deleteById(id)
    }
}
