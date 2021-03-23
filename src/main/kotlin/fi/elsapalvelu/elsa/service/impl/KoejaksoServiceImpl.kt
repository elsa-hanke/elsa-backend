package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KoejaksoRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.KoejaksoService
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksoMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
@Transactional
class KoejaksoServiceImpl(
    private val koejaksoRepository: KoejaksoRepository,
    private val koejaksoMapper: KoejaksoMapper,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper
) : KoejaksoService {

    override fun save(koejaksoDTO: KoejaksoDTO): KoejaksoDTO {
        var koejakso = koejaksoMapper.toEntity(koejaksoDTO)
        koejakso = koejaksoRepository.save(koejakso)
        return koejaksoMapper.toDto(koejakso)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<KoejaksoDTO> {
        return koejaksoRepository.findAll()
            .mapTo(mutableListOf(), koejaksoMapper::toDto)
    }

    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): KoejaksoDTO {
        val koejakso = koejaksoRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
        return koejaksoMapper.toDto(koejakso)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksoDTO> {
        return koejaksoRepository.findById(id)
            .map(koejaksoMapper::toDto)
    }

    override fun delete(id: Long) {
        koejaksoRepository.deleteById(id)
    }

    override fun addTyoskentelyjakso(
        koejaksoId: Long,
        tyoskentelyjaksoId: Long,
        userId: String
    ): TyoskentelyjaksoDTO {
        val koejakso = koejaksoRepository.findById(koejaksoId).orElseThrow {
            BadRequestAlertException(
                "Epäkelvollinen id",
                "koejakso",
                "idnull"
            )
        }
        val tyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId).orElseThrow {
            BadRequestAlertException(
                "Epäkelvollinen id",
                "tyojakso",
                "idnull"
            )
        }

        if (tyoskentelyjakso.erikoistuvaLaakari?.kayttaja?.user?.id == userId) {
            tyoskentelyjakso.koejakso = koejakso
            tyoskentelyjaksoRepository.save(tyoskentelyjakso)
        }

        return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
    }

    override fun removeTyoskentelyjakso(
        koejaksoId: Long,
        tyoskentelyjaksoId: Long,
        userId: String
    ) {
        val tyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjaksoId).orElseThrow {
            BadRequestAlertException(
                "Epäkelvollinen id",
                "tyojakso",
                "idnull"
            )
        }

        if (tyoskentelyjakso.erikoistuvaLaakari?.kayttaja?.user?.id == userId && tyoskentelyjakso.koejakso?.id == koejaksoId) {
            tyoskentelyjakso.koejakso = null
            tyoskentelyjaksoRepository.save(tyoskentelyjakso)
        }
    }
}
