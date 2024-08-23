package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.PoissaolonSyyRepository
import fi.elsapalvelu.elsa.service.PoissaolonSyyService
import fi.elsapalvelu.elsa.service.dto.PoissaolonSyyDTO
import fi.elsapalvelu.elsa.service.mapper.PoissaolonSyyMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class PoissaolonSyyServiceImpl(
    private val poissaolonSyyRepository: PoissaolonSyyRepository,
    private val poissaolonSyyMapper: PoissaolonSyyMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : PoissaolonSyyService {

    override fun save(poissaolonSyyDTO: PoissaolonSyyDTO): PoissaolonSyyDTO {
        var poissaolonSyy = poissaolonSyyMapper.toEntity(poissaolonSyyDTO)
        poissaolonSyy = poissaolonSyyRepository.save(poissaolonSyy)
        return poissaolonSyyMapper.toDto(poissaolonSyy)
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<PoissaolonSyyDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä ei ole määritetty, käytetään nykyistä päivää
            // voimassaolon rajaamisessa
            return poissaolonSyyRepository.findAllByValid(
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(poissaolonSyyMapper::toDto)
        } ?: listOf()
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): PoissaolonSyyDTO? {
        poissaolonSyyRepository.findByIdOrNull(id)?.let {
            return poissaolonSyyMapper.toDto(it)
        }
        return null
    }

    override fun delete(id: Long) {
        poissaolonSyyRepository.deleteById(id)
    }

    override fun findAll(): List<PoissaolonSyyDTO> {
        poissaolonSyyRepository.findAll().let {
            return poissaolonSyyMapper.toDto(it)
        }
    }

}
