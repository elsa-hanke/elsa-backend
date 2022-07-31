package fi.elsapalvelu.elsa.service.impl

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
            val result = opintoopasMapper.toDto(it)

            result.kaytannonKoulutuksenVahimmaispituusVuodet =
                getYears(it.kaytannonKoulutuksenVahimmaispituus)
            result.kaytannonKoulutuksenVahimmaispituusKuukaudet =
                getMonths(it.kaytannonKoulutuksenVahimmaispituus)

            result.terveyskeskuskoulutusjaksonVahimmaispituusVuodet =
                getYears(it.terveyskeskuskoulutusjaksonVahimmaispituus)
            result.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet =
                getMonths(it.terveyskeskuskoulutusjaksonVahimmaispituus)

            if (it.terveyskeskuskoulutusjaksonMaksimipituus != null) {
                result.terveyskeskuskoulutusjaksonMaksimipituusVuodet =
                    getYears(it.terveyskeskuskoulutusjaksonMaksimipituus)
                result.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet =
                    getMonths(it.terveyskeskuskoulutusjaksonMaksimipituus)
            }

            result.yliopistosairaalajaksonVahimmaispituusVuodet =
                getYears(it.yliopistosairaalajaksonVahimmaispituus)
            result.yliopistosairaalajaksonVahimmaispituusKuukaudet =
                getMonths(it.yliopistosairaalajaksonVahimmaispituus)

            result.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet =
                getYears(it.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus)
            result.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet =
                getMonths(it.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus)

            return result
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
        val result = opintoopasRepository.save(opintoopas)

        return opintoopasMapper.toDto(result)
    }

    private fun getYears(days: Double?): Int? {
        return days?.let { it / DAYS_IN_YEAR }?.toInt()
    }

    private fun getMonths(days: Double?): Int? {
        return days?.let { (it % DAYS_IN_YEAR) / DAYS_IN_YEAR * MONTHS_IN_YEAR }?.toInt()
    }

    private fun toDays(years: Int?, months: Int?): Double {
        return ((years ?: 0) * DAYS_IN_YEAR) + ((months
            ?: 0).toDouble() / MONTHS_IN_YEAR * DAYS_IN_YEAR)
    }
}
