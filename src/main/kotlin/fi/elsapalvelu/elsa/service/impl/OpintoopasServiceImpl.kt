package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintoopas
import fi.elsapalvelu.elsa.extensions.toMonths
import fi.elsapalvelu.elsa.extensions.toYears
import fi.elsapalvelu.elsa.repository.OpintoopasRepository
import fi.elsapalvelu.elsa.service.OpintoopasService
import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import fi.elsapalvelu.elsa.service.dto.OpintoopasSimpleDTO
import fi.elsapalvelu.elsa.service.mapper.OpintoopasMapper
import fi.elsapalvelu.elsa.service.mapper.OpintoopasSimpleMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DAYS_IN_YEAR = 365
private const val MONTHS_IN_YEAR = 12

@Service
@Transactional
class OpintoopasServiceImpl(
    private val opintoopasRepository: OpintoopasRepository,
    private val opintoopasMapper: OpintoopasMapper,
    private val opintoopasSimpleMapper: OpintoopasSimpleMapper
) : OpintoopasService {

    override fun findOne(id: Long): OpintoopasDTO? {
        return opintoopasRepository.findByIdOrNull(id)?.let {
            return mapOpintoopas(it)
        }
    }

    override fun findAll(): List<OpintoopasSimpleDTO> {
        return opintoopasRepository.findAll()
            .map(opintoopasSimpleMapper::toDto)
    }

    override fun findAllByErikoisala(erikoisalaId: Long): List<OpintoopasSimpleDTO> {
        return opintoopasRepository.findAllByErikoisalaId(erikoisalaId)
            .map(opintoopasSimpleMapper::toDto)
    }

    override fun findUusinByErikoisala(erikoisalaId: Long): OpintoopasDTO? {
        opintoopasRepository.findFirstByErikoisalaIdOrderByVoimassaoloAlkaaDesc(erikoisalaId)?.let {
            return mapOpintoopas(it)
        }
        return null
    }

    override fun update(opintoopasDTO: OpintoopasDTO): OpintoopasDTO? {
        val uusinOpas =
            opintoopasRepository.findFirstByErikoisalaIdOrderByVoimassaoloAlkaaDesc(opintoopasDTO.erikoisala!!.id!!)
        if (uusinOpas != null && uusinOpas.id != opintoopasDTO.id && opintoopasDTO.voimassaoloAlkaa!!.isAfter(
                uusinOpas.voimassaoloAlkaa
            ) && uusinOpas.voimassaoloPaattyy == null
        ) {
            uusinOpas.voimassaoloPaattyy = opintoopasDTO.voimassaoloAlkaa!!.minusDays(1)
            opintoopasRepository.save(uusinOpas)
        }
        val opintoopas = opintoopasMapper.toEntity(opintoopasDTO)
        opintoopas.kaytannonKoulutuksenVahimmaispituus = toDays(
            opintoopasDTO.kaytannonKoulutuksenVahimmaispituusVuodet,
            opintoopasDTO.kaytannonKoulutuksenVahimmaispituusKuukaudet
        )
        opintoopas.terveyskeskuskoulutusjaksonVahimmaispituus = toDays(
            opintoopasDTO.terveyskeskuskoulutusjaksonVahimmaispituusVuodet,
            opintoopasDTO.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet
        )
        if (opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusVuodet != null || opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet != null) {
            opintoopas.terveyskeskuskoulutusjaksonMaksimipituus = toDays(
                opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusVuodet,
                opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet
            )
        }
        opintoopas.yliopistosairaalajaksonVahimmaispituus = toDays(
            opintoopasDTO.yliopistosairaalajaksonVahimmaispituusVuodet,
            opintoopasDTO.yliopistosairaalajaksonVahimmaispituusKuukaudet
        )
        opintoopas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus = toDays(
            opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet,
            opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet
        )
        if (opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet != null || opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet != null) {
            opintoopas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituus = toDays(
                opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet,
                opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet
            )
        }
        val result = opintoopasRepository.save(opintoopas)

        return opintoopasMapper.toDto(result)
    }

    private fun mapOpintoopas(opas: Opintoopas): OpintoopasDTO {
        val dto = opintoopasMapper.toDto(opas)
        dto.kaytannonKoulutuksenVahimmaispituusVuodet =
            opas.kaytannonKoulutuksenVahimmaispituus?.toYears()
        dto.kaytannonKoulutuksenVahimmaispituusKuukaudet =
            opas.kaytannonKoulutuksenVahimmaispituus?.toMonths()

        dto.terveyskeskuskoulutusjaksonVahimmaispituusVuodet =
            opas.terveyskeskuskoulutusjaksonVahimmaispituus?.toYears()
        dto.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet =
            opas.terveyskeskuskoulutusjaksonVahimmaispituus?.toMonths()

        if (opas.terveyskeskuskoulutusjaksonMaksimipituus != null) {
            dto.terveyskeskuskoulutusjaksonMaksimipituusVuodet =
                opas.terveyskeskuskoulutusjaksonMaksimipituus?.toYears()
            dto.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet =
                opas.terveyskeskuskoulutusjaksonMaksimipituus?.toMonths()
        }

        dto.yliopistosairaalajaksonVahimmaispituusVuodet =
            opas.yliopistosairaalajaksonVahimmaispituus?.toYears()
        dto.yliopistosairaalajaksonVahimmaispituusKuukaudet =
            opas.yliopistosairaalajaksonVahimmaispituus?.toMonths()

        dto.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet =
            opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus?.toYears()
        dto.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet =
            opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus?.toMonths()

        if (opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituus != null) {
            dto.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusVuodet =
                opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituus?.toYears()
            dto.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituusKuukaudet =
                opas.yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituus?.toMonths()
        }
        return dto
    }

    private fun toDays(years: Int?, months: Int?): Double {
        return ((years ?: 0) * DAYS_IN_YEAR) + ((months
            ?: 0).toDouble() / MONTHS_IN_YEAR * DAYS_IN_YEAR)
    }
}
