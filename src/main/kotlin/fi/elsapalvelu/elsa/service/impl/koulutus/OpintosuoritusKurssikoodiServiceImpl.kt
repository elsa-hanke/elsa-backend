package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusKurssikoodiRepository
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusKurssikooditService
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusKurssikoodiDTO
import fi.elsapalvelu.elsa.service.mapper.koulutus.OpintosuoritusKurssikoodiMapper
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
                opintosuoritusKurssikoodiDTO.id.required(),
                yliopisto?.nimi.required()
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
            val result = opintosuoritusKurssikoodiRepository.findAllByYliopistoNimi(it.nimi.required())
            return result.map(opintosuoritusKurssikoodiMapper::toDto)
        }
        return null
    }

    override fun findOne(id: Long, userId: String): Optional<OpintosuoritusKurssikoodiDTO> {
        getYliopisto(userId)?.let { yliopisto ->
            opintosuoritusKurssikoodiRepository.findByIdAndYliopistoNimi(id, yliopisto.nimi.required())
                ?.let {
                    return Optional.of(opintosuoritusKurssikoodiMapper.toDto(it))
                }
        }
        return Optional.empty()
    }

    override fun delete(id: Long, userId: String) {
        getYliopisto(userId)?.let { yliopisto ->
            opintosuoritusKurssikoodiRepository.findByIdAndYliopistoNimi(id, yliopisto.nimi.required())
                ?.let {
                    opintosuoritusKurssikoodiRepository.deleteById(it.id.required())
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
