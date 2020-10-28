package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class TyoskentelyjaksoServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper
) : TyoskentelyjaksoService {

    override fun save(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): TyoskentelyjaksoDTO {
        var tyoskentelyjakso = tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO)
        tyoskentelyjakso = tyoskentelyjaksoRepository.save(tyoskentelyjakso)
        return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<TyoskentelyjaksoDTO> {
        return tyoskentelyjaksoRepository.findAll()
            .mapTo(mutableListOf(), tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(id: String): MutableList<TyoskentelyjaksoDTO> {
        return tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(id)
            .mapTo(mutableListOf(), tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<TyoskentelyjaksoDTO> {
        return tyoskentelyjaksoRepository.findById(id)
            .map(tyoskentelyjaksoMapper::toDto)
    }

    override fun delete(id: Long) {
        tyoskentelyjaksoRepository.deleteById(id)
    }
}
