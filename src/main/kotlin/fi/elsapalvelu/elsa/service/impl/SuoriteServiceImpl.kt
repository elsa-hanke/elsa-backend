package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.SuoriteRepository
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.service.SuoriteService
import fi.elsapalvelu.elsa.service.dto.SuoriteDTO
import fi.elsapalvelu.elsa.service.dto.SuoriteWithErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoriteMapper
import fi.elsapalvelu.elsa.service.mapper.SuoriteWithErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritteenKategoriaWithErikoisalaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class SuoriteServiceImpl(
    private val suoriteRepository: SuoriteRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val suoriteMapper: SuoriteMapper,
    private val suoriteWithErikoisalaMapper: SuoriteWithErikoisalaMapper,
    private val suoritteenKategoriaWithErikoisalaMapper: SuoritteenKategoriaWithErikoisalaMapper,
    private val suoritemerkintaRepository: SuoritemerkintaRepository
) : SuoriteService {

    override fun create(suoriteDTO: SuoriteWithErikoisalaDTO): SuoriteWithErikoisalaDTO {
        // Korvataan edellinen
        if (suoriteDTO.id != null) {
            suoriteRepository.findById(suoriteDTO.id!!).orElse(null)
                ?.let {
                    it.voimassaolonPaattymispaiva =
                        suoriteDTO.voimassaolonAlkamispaiva?.minusDays(1)
                    suoriteRepository.save(it)
                }
        }

        suoriteDTO.id = null
        var suorite = suoriteWithErikoisalaMapper.toEntity(suoriteDTO)
        suorite = suoriteRepository.save(suorite)
        return suoriteWithErikoisalaMapper.toDto(suorite)
    }

    override fun update(suoriteDTO: SuoriteWithErikoisalaDTO): SuoriteWithErikoisalaDTO? {
        return suoriteRepository.findById(suoriteDTO.id!!).orElse(null)
            ?.let {
                it.nimi = suoriteDTO.nimi
                it.nimiSv = suoriteDTO.nimiSv
                it.voimassaolonAlkamispaiva = suoriteDTO.voimassaolonAlkamispaiva
                it.voimassaolonPaattymispaiva = suoriteDTO.voimassaolonPaattymispaiva
                it.vaadittulkm = suoriteDTO.vaadittulkm
                suoriteDTO.kategoria?.let { kategoria ->
                    it.kategoria = suoritteenKategoriaWithErikoisalaMapper.toEntity(kategoria)
                }

                val result = suoriteRepository.save(it)

                suoriteWithErikoisalaMapper.toDto(result)
            }
    }

    @Transactional(readOnly = true)
    override fun findAllOpintooikeusId(opintooikeusId: Long): List<SuoriteDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            // Jos päivämäärää jonka mukainen osaamisen arvioinnin opas käytössä ei ole määritetty, käytetään nykyistä päivää
            // voimassaolon rajaamisessa
            return suoriteRepository.findAllByValid(
                it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(suoriteMapper::toDto)
        } ?: listOf()
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoriteWithErikoisalaDTO> {
        val result = suoriteRepository.findById(id)
            .map(suoriteWithErikoisalaMapper::toDto)
        result.ifPresent {
            it.voiPoistaa = !suoritemerkintaRepository.existsBySuoriteId(it.id!!)
        }
        return result
    }

    override fun delete(id: Long) {
        suoriteRepository.deleteById(id)
    }
}
