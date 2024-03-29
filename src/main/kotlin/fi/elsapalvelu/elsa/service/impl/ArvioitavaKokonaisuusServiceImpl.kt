package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavaKokonaisuusRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.ArvioitavaKokonaisuusService
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusWithErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaKokonaisuusWithErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaSimpleMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class ArvioitavaKokonaisuusServiceImpl(
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper,
    private val arvioitavaKokonaisuusWithErikoisalaMapper: ArvioitavaKokonaisuusWithErikoisalaMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val arvioitavanKokonaisuudenKategoriaSimpleMapper: ArvioitavanKokonaisuudenKategoriaSimpleMapper,
    private val suoritusarviointiRepository: SuoritusarviointiRepository
) : ArvioitavaKokonaisuusService {

    override fun create(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO {

        // Korvataan edellinen
        if (arvioitavaKokonaisuusDTO.id != null) {
            arvioitavaKokonaisuusRepository.findById(arvioitavaKokonaisuusDTO.id!!).orElse(null)
                ?.let {
                    it.voimassaoloLoppuu = arvioitavaKokonaisuusDTO.voimassaoloAlkaa?.minusDays(1)
                    arvioitavaKokonaisuusRepository.save(it)
                }
        }

        arvioitavaKokonaisuusDTO.id = null
        var arvioitavaKokonaisuus = arvioitavaKokonaisuusMapper.toEntity(arvioitavaKokonaisuusDTO)
        arvioitavaKokonaisuus = arvioitavaKokonaisuusRepository.save(arvioitavaKokonaisuus)
        return arvioitavaKokonaisuusMapper.toDto(arvioitavaKokonaisuus)
    }

    override fun update(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO? {
        return arvioitavaKokonaisuusRepository.findById(arvioitavaKokonaisuusDTO.id!!).orElse(null)
            ?.let {
                it.nimi = arvioitavaKokonaisuusDTO.nimi
                it.nimiSv = arvioitavaKokonaisuusDTO.nimiSv
                it.voimassaoloAlkaa = arvioitavaKokonaisuusDTO.voimassaoloAlkaa
                it.voimassaoloLoppuu = arvioitavaKokonaisuusDTO.voimassaoloLoppuu
                it.kuvaus = arvioitavaKokonaisuusDTO.kuvaus
                it.kuvausSv = arvioitavaKokonaisuusDTO.kuvausSv
                arvioitavaKokonaisuusDTO.kategoria?.let { kategoria ->
                    it.kategoria = arvioitavanKokonaisuudenKategoriaSimpleMapper.toEntity(kategoria)
                }

                val result = arvioitavaKokonaisuusRepository.save(it)

                arvioitavaKokonaisuusMapper.toDto(result)
            }
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavaKokonaisuusDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            // Jos päivämäärää jonka mukainen osaamisen arvioinnin opas käytössä, ei ole määritetty, käytetään nykyistä päivää
            // voimassaolon rajaamisessa
            arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
                it.erikoisala?.id, it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(arvioitavaKokonaisuusMapper::toDto)
        } ?: listOf()
    }

    override fun findAllByErikoisalaIdPaged(
        erikoisalaId: Long?,
        voimassaolevat: Boolean?,
        pageable: Pageable
    ): Page<ArvioitavaKokonaisuusDTO> {
        val now = LocalDate.now()
        val arvioitavatKokonaisuudet =
            if (voimassaolevat == true) arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
                erikoisalaId,
                now,
                pageable
            )
            else arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndExpired(
                erikoisalaId,
                now,
                pageable
            )
        return arvioitavatKokonaisuudet.map(arvioitavaKokonaisuusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArvioitavaKokonaisuusWithErikoisalaDTO> {
        val result = arvioitavaKokonaisuusRepository.findById(id)
            .map(arvioitavaKokonaisuusWithErikoisalaMapper::toDto)
        result.ifPresent {
            it.voiPoistaa =
                !suoritusarviointiRepository.existsByArvioitavatKokonaisuudetArvioitavaKokonaisuusId(
                    it.id!!
                )
        }
        return result
    }

    override fun delete(id: Long) {
        arvioitavaKokonaisuusRepository.deleteById(id)
    }
}
