package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.KeskeytysaikaService
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KeskeytysaikaServiceImpl(
    private val keskeytysaikaRepository: KeskeytysaikaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val keskeytysaikaMapper: KeskeytysaikaMapper
) : KeskeytysaikaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(keskeytysaikaDTO: KeskeytysaikaDTO, userId: String): KeskeytysaikaDTO? {
        log.debug("Request to save Keskeytysaika : $keskeytysaikaDTO")

        tyoskentelyjaksoRepository.findByIdOrNull(keskeytysaikaDTO.tyoskentelyjaksoId)?.let { tyoskentelyjakso ->
            if (
                userId == tyoskentelyjakso.erikoistuvaLaakari?.kayttaja?.user?.id && (
                    tyoskentelyjakso.alkamispaiva!!.isBefore(keskeytysaikaDTO.alkamispaiva) ||
                        tyoskentelyjakso.alkamispaiva!!.isEqual(keskeytysaikaDTO.alkamispaiva)
                    )
            ) {
                if (
                    tyoskentelyjakso.paattymispaiva != null &&
                    tyoskentelyjakso.paattymispaiva!!.isBefore(keskeytysaikaDTO.paattymispaiva)
                ) {
                    return null
                }

                if (
                    keskeytysaikaDTO.osaaikaprosentti!! > tyoskentelyjakso.osaaikaprosentti!!
                ) {
                    return null
                }

                var keskeytysaika = keskeytysaikaMapper.toEntity(keskeytysaikaDTO)
                keskeytysaika = keskeytysaikaRepository.save(keskeytysaika)
                return keskeytysaikaMapper.toDto(keskeytysaika)
            }
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): MutableList<KeskeytysaikaDTO> {
        return keskeytysaikaRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(userId)
            .mapTo(mutableListOf(), keskeytysaikaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): KeskeytysaikaDTO? {
        log.debug("Request to get Keskeytysaika : $id")

        keskeytysaikaRepository.findByIdOrNull(id)?.let { keskeytysaika ->
            if (keskeytysaika.tyoskentelyjakso?.erikoistuvaLaakari?.kayttaja?.user?.id == userId) {
                return keskeytysaikaMapper.toDto(keskeytysaika)
            }
        }
        return null
    }

    override fun delete(id: Long, userId: String) {
        log.debug("Request to delete Keskeytysaika : $id")

        keskeytysaikaRepository.findByIdOrNull(id)?.let { keskeytysaika ->
            keskeytysaika.tyoskentelyjakso?.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        keskeytysaikaRepository.deleteById(id)
                    }
                }
            }
        }
    }
}
