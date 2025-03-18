package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArviointityokaluKategoriaRepository
import fi.elsapalvelu.elsa.repository.ArviointityokaluRepository
import fi.elsapalvelu.elsa.service.ArviointityokaluKategoriaService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKategoriaMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class ArviointityokaluKategoriaServiceImpl(
    private val arviointityokaluKategoriaRepository: ArviointityokaluKategoriaRepository,
    private val arviointityokaluKategoriaMapper: ArviointityokaluKategoriaMapper,
    private val arviointityokaluRepository: ArviointityokaluRepository
) : ArviointityokaluKategoriaService {

    override fun create(arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO): ArviointityokaluKategoriaDTO {
        var arviointityokalu = arviointityokaluKategoriaMapper.toEntity(arviointityokaluKategoriaDTO)
        arviointityokalu.luontiaika = Instant.now()
        arviointityokalu.muokkausaika = Instant.now()
        arviointityokalu = arviointityokaluKategoriaRepository.save(arviointityokalu)
        return arviointityokaluKategoriaMapper.toDto(arviointityokalu)
    }

    override fun update(arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO): ArviointityokaluKategoriaDTO? {
        return arviointityokaluKategoriaRepository.findById(arviointityokaluKategoriaDTO.id!!).orElse(null)
            ?.let {
                it.nimi = arviointityokaluKategoriaDTO.nimi
                val result = arviointityokaluKategoriaRepository.save(it)
                arviointityokaluKategoriaMapper.toDto(result)
            }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ArviointityokaluKategoriaDTO> {
        return arviointityokaluKategoriaRepository.findAllByKaytossaTrue()
            .map(arviointityokaluKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArviointityokaluKategoriaDTO> {
        return arviointityokaluKategoriaRepository.findById(id)
            .map(arviointityokaluKategoriaMapper::toDto)
    }

    override fun delete(id: Long) {
        arviointityokaluKategoriaRepository.findById(id).ifPresent { arviointityokaluKategoria ->
            arviointityokaluRepository.findAllByKategoria(arviointityokaluKategoria).forEach { arviointityokalu ->
                arviointityokalu.kategoria = null
                arviointityokaluRepository.save(arviointityokalu)
            }
            arviointityokaluKategoria.kaytossa = false
            arviointityokaluKategoriaRepository.save(arviointityokaluKategoria)
        }
    }
}
