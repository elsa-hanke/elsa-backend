package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.OpintosuoritusKurssikoodiRepository
import fi.elsapalvelu.elsa.service.OpintosuoritusKurssikooditService
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusKurssikoodiDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusKurssikoodiMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class OpintosuoritusKurssikoodiServiceImpl(
    private val opintosuoritusKurssikoodiRepository: OpintosuoritusKurssikoodiRepository,
    private val opintosuoritusKurssikoodiMapper: OpintosuoritusKurssikoodiMapper,
    private val kayttajaRepository: KayttajaRepository
) : OpintosuoritusKurssikooditService {

    override fun save(
        userId: String,
        opintosuoritusKurssikoodiDTO: OpintosuoritusKurssikoodiDTO
    ): OpintosuoritusKurssikoodiDTO? {
        val yliopisto = getYliopisto(userId)

        if (opintosuoritusKurssikoodiDTO.id != null) {
            opintosuoritusKurssikoodiRepository.findByIdAndYliopistoNimi(
                opintosuoritusKurssikoodiDTO.id!!,
                yliopisto?.nimi!!
            )?.let {
                it.tunniste = opintosuoritusKurssikoodiDTO.tunniste
                opintosuoritusKurssikoodiRepository.save(it)
                return opintosuoritusKurssikoodiMapper.toDto(it)
            }
        } else {
            val kurssikoodi = opintosuoritusKurssikoodiMapper.toEntity(opintosuoritusKurssikoodiDTO)
            kurssikoodi.yliopisto = yliopisto
            kurssikoodi.isOsakokonaisuus = false
            val result = opintosuoritusKurssikoodiRepository.save(kurssikoodi)
            return opintosuoritusKurssikoodiMapper.toDto(result)
        }

        return null
    }

    override fun findAllForVirkailija(userId: String): List<OpintosuoritusKurssikoodiDTO>? {
        getYliopisto(userId)?.let {
            val result = opintosuoritusKurssikoodiRepository.findAllByYliopistoNimi(it.nimi!!)
            return result.map(opintosuoritusKurssikoodiMapper::toDto)
        }
        return null
    }

    override fun findOne(id: Long, userId: String): Optional<OpintosuoritusKurssikoodiDTO> {
        getYliopisto(userId)?.let { yliopisto ->
            opintosuoritusKurssikoodiRepository.findByIdAndYliopistoNimi(id, yliopisto.nimi!!)
                ?.let {
                    return Optional.of(opintosuoritusKurssikoodiMapper.toDto(it))
                }
        }
        return Optional.empty()
    }

    override fun delete(id: Long, userId: String) {
        getYliopisto(userId)?.let { yliopisto ->
            opintosuoritusKurssikoodiRepository.findByIdAndYliopistoNimi(id, yliopisto.nimi!!)
                ?.let {
                    opintosuoritusKurssikoodiRepository.deleteById(it.id!!)
                }
        }
    }

    private fun getYliopisto(userId: String): Yliopisto? {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let {
            return it.yliopistot.first()
        }
        return null
    }

}
