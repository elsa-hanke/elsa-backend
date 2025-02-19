package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymysVaihtoehto
import fi.elsapalvelu.elsa.repository.ArviointityokaluKategoriaRepository
import fi.elsapalvelu.elsa.repository.ArviointityokaluRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKysymysMapper
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKysymysVaihtoehtoMapper
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class ArviointityokaluServiceImpl(
    private val arviointityokaluRepository: ArviointityokaluRepository,
    private val arviointityokaluMapper: ArviointityokaluMapper,
    private val arviointityokaluKysymysMapper: ArviointityokaluKysymysMapper,
    private val arviointityokaluKysymysVaihtoehtoMapper: ArviointityokaluKysymysVaihtoehtoMapper,
    private val arviointityokaluKategoriaRepository: ArviointityokaluKategoriaRepository,
    private val kayttajaRepository: KayttajaRepository
) : ArviointityokaluService {


    override fun save(
        arviointityokaluDTO: ArviointityokaluDTO,
        user: UserDTO
    ): ArviointityokaluDTO {
        var arviointityokalu = arviointityokaluMapper.toEntity(arviointityokaluDTO)
        val now = Instant.now()
        if (arviointityokalu.id == null) {
            arviointityokalu.luontiaika = now
        }
        arviointityokalu.muokkausaika = now
        arviointityokalu.kayttaja = user.id?.let { kayttajaRepository.findOneByUserId(it).orElse(null) }
        arviointityokalu.kategoria = arviointityokalu.kategoria?.let {
            arviointityokaluKategoriaRepository.findById(it.id!!).orElse(null)
        }
        arviointityokalu.kysymykset.forEach { kysymys ->
            kysymys.arviointityokalu = arviointityokalu
            kysymys.vaihtoehdot.forEach { vaihtoehto ->
                vaihtoehto.arviointityokaluKysymys = kysymys
            }
        }
        arviointityokalu = arviointityokaluRepository.save(arviointityokalu)
        return arviointityokaluMapper.toDto(arviointityokalu)
    }


    @Transactional(readOnly = true)
    override fun findAll(): List<ArviointityokaluDTO> {
        return arviointityokaluRepository.findAllByKaytossaTrue()
            .map(arviointityokaluMapper::toDto)
    }

    override fun findAllByKayttajaUserId(userId: String): MutableList<ArviointityokaluDTO> {
        return arviointityokaluRepository.findAllByKayttajaIsNullOrKayttajaUserId(userId)
            .mapTo(mutableListOf(), arviointityokaluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArviointityokaluDTO> {
        return arviointityokaluRepository.findById(id)
            .map(arviointityokaluMapper::toDto)
    }

    override fun delete(id: Long) {
        arviointityokaluRepository.deleteById(id)
    }

    override fun update(arviointityokaluDTO: ArviointityokaluDTO): ArviointityokaluDTO? {
        return arviointityokaluRepository.findById(arviointityokaluDTO.id!!)
            .orElse(null)?.let { arviointityokalu ->
                arviointityokalu.nimi = arviointityokaluDTO.nimi
                arviointityokalu.ohjeteksti = arviointityokaluDTO.ohjeteksti
                arviointityokaluDTO.kategoria?.let {
                    arviointityokalu.kategoria = arviointityokaluKategoriaRepository.findById(it.id!!).orElse(null)
                }
                val existingKysymykset = arviointityokalu.kysymykset.associateBy { it.id }
                val updatedKysymykset = mutableSetOf<ArviointityokaluKysymys>()
                arviointityokaluDTO.kysymykset?.forEach { kysymysDTO ->
                    val kysymys =
                        existingKysymykset[kysymysDTO.id] ?: arviointityokaluKysymysMapper.toEntity(kysymysDTO)
                    kysymys.arviointityokalu = arviointityokalu
                    kysymys.otsikko = kysymysDTO.otsikko
                    kysymys.pakollinen = kysymysDTO.pakollinen
                    kysymys.jarjestysnumero = kysymysDTO.jarjestysnumero
                    val existingVaihtoehdot = kysymys.vaihtoehdot.associateBy { it.id }
                    val updatedVaihtoehdot = mutableSetOf<ArviointityokaluKysymysVaihtoehto>()
                    kysymysDTO.vaihtoehdot?.forEach { vaihtoehtoDTO ->
                        val vaihtoehto = existingVaihtoehdot[vaihtoehtoDTO.id]
                            ?: arviointityokaluKysymysVaihtoehtoMapper.toEntity(vaihtoehtoDTO)
                        vaihtoehto.arviointityokaluKysymys = kysymys
                        vaihtoehto.valittu = vaihtoehtoDTO.valittu
                        vaihtoehto.teksti = vaihtoehtoDTO.teksti
                        updatedVaihtoehdot.add(vaihtoehto)
                    }
                    kysymys.vaihtoehdot.clear()
                    kysymys.vaihtoehdot.addAll(updatedVaihtoehdot)
                    updatedKysymykset.add(kysymys)
                }
                arviointityokalu.kysymykset.clear()
                arviointityokalu.kysymykset.addAll(updatedKysymykset)
                // arviointityokalu.liite = todo
                arviointityokalu.muokkausaika = Instant.now()
                val result = arviointityokaluRepository.save(arviointityokalu)
                arviointityokaluMapper.toDto(result)
            }
    }
}
