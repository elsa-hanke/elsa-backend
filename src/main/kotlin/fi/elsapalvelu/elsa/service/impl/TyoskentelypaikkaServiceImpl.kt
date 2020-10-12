package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.repository.TyoskentelypaikkaRepository
import fi.elsapalvelu.elsa.service.TyoskentelypaikkaService
import fi.elsapalvelu.elsa.service.dto.TyoskentelypaikkaDTO
import fi.elsapalvelu.elsa.service.mapper.TyoskentelypaikkaMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Tyoskentelypaikka].
 */
@Service
@Transactional
class TyoskentelypaikkaServiceImpl(
    private val tyoskentelypaikkaRepository: TyoskentelypaikkaRepository,
    private val tyoskentelypaikkaMapper: TyoskentelypaikkaMapper
) : TyoskentelypaikkaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(tyoskentelypaikkaDTO: TyoskentelypaikkaDTO): TyoskentelypaikkaDTO {
        log.debug("Request to save Tyoskentelypaikka : $tyoskentelypaikkaDTO")

        var tyoskentelypaikka = tyoskentelypaikkaMapper.toEntity(tyoskentelypaikkaDTO)
        tyoskentelypaikka = tyoskentelypaikkaRepository.save(tyoskentelypaikka)
        return tyoskentelypaikkaMapper.toDto(tyoskentelypaikka)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<TyoskentelypaikkaDTO> {
        log.debug("Request to get all Tyoskentelypaikkas")
        return tyoskentelypaikkaRepository.findAll()
            .mapTo(mutableListOf(), tyoskentelypaikkaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<TyoskentelypaikkaDTO> {
        log.debug("Request to get Tyoskentelypaikka : $id")
        return tyoskentelypaikkaRepository.findById(id)
            .map(tyoskentelypaikkaMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Tyoskentelypaikka : $id")

        tyoskentelypaikkaRepository.deleteById(id)
    }
}
