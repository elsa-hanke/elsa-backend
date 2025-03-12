package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymysVaihtoehto
import fi.elsapalvelu.elsa.domain.AsiakirjaData
import fi.elsapalvelu.elsa.repository.ArviointityokaluKategoriaRepository
import fi.elsapalvelu.elsa.repository.ArviointityokaluRepository
import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.*

@Service
@Transactional
class ArviointityokaluServiceImpl(
    private val arviointityokaluRepository: ArviointityokaluRepository,
    private val arviointityokaluMapper: ArviointityokaluMapper,
    private val arviointityokaluKategoriaRepository: ArviointityokaluKategoriaRepository,
) : ArviointityokaluService {

    override fun save(
        arviointityokaluDTO: ArviointityokaluDTO,
        user: UserDTO,
        liiteData: MultipartFile?
    ): ArviointityokaluDTO {
        var arviointityokalu = arviointityokaluMapper.toEntity(arviointityokaluDTO)
        val now = Instant.now()
        if (arviointityokalu.id == null) {
            arviointityokalu.luontiaika = now
        }
        arviointityokalu.muokkausaika = now
        arviointityokalu.kayttaja = null
        arviointityokalu.kategoria = arviointityokalu.kategoria?.let {
            arviointityokaluKategoriaRepository.findById(it.id!!).orElse(null)
        }
        arviointityokalu.kysymykset.forEach { kysymys ->
            kysymys.arviointityokalu = arviointityokalu
            kysymys.vaihtoehdot.forEach { vaihtoehto ->
                vaihtoehto.arviointityokaluKysymys = kysymys
            }
        }
        if (liiteData != null) {
            arviointityokalu.liite = AsiakirjaData(data = liiteData.bytes)
            arviointityokalu.liitetiedostonNimi = liiteData.originalFilename
            arviointityokalu.liitetiedostonTyyppi = liiteData.contentType
        }
        arviointityokalu = arviointityokaluRepository.save(arviointityokalu)
        return arviointityokaluMapper.toDto(arviointityokalu)
    }

    override fun update(
        arviointityokaluDTO: ArviointityokaluDTO,
        liiteData: MultipartFile?
    ): ArviointityokaluDTO? {
        return arviointityokaluRepository.findById(arviointityokaluDTO.id!!)
            .orElse(null)?.let { arviointityokalu ->
                arviointityokalu.nimi = arviointityokaluDTO.nimi
                arviointityokalu.ohjeteksti = arviointityokaluDTO.ohjeteksti
                arviointityokalu.tila = arviointityokaluDTO.tila
                arviointityokaluDTO.kategoria?.let {
                    arviointityokalu.kategoria = arviointityokaluKategoriaRepository.findById(it.id!!).orElse(null)
                }
                val existingKysymykset = arviointityokalu.kysymykset.associateBy { it.id }
                val updatedKysymykset = mutableSetOf<ArviointityokaluKysymys>()
                arviointityokaluDTO.kysymykset?.forEach { kysymysDTO ->
                    val kysymys = existingKysymykset[kysymysDTO.id] ?: ArviointityokaluKysymys(
                        id = null,
                        arviointityokalu = arviointityokalu,
                        otsikko = kysymysDTO.otsikko,
                        pakollinen = kysymysDTO.pakollinen,
                        jarjestysnumero = kysymysDTO.jarjestysnumero
                    )
                    kysymys.otsikko = kysymysDTO.otsikko
                    kysymys.pakollinen = kysymysDTO.pakollinen
                    kysymys.jarjestysnumero = kysymysDTO.jarjestysnumero
                    kysymys.arviointityokalu = arviointityokalu
                    kysymys.tyyppi = kysymysDTO.tyyppi
                    val existingVaihtoehdot = kysymys.vaihtoehdot.associateBy { it.id }
                    val updatedVaihtoehdot = mutableListOf<ArviointityokaluKysymysVaihtoehto>()
                    kysymysDTO.vaihtoehdot?.forEach { vaihtoehtoDTO ->
                        val vaihtoehto = existingVaihtoehdot[vaihtoehtoDTO.id] ?: ArviointityokaluKysymysVaihtoehto(
                            id = null,
                            teksti = vaihtoehtoDTO.teksti,
                            valittu = vaihtoehtoDTO.valittu,
                            arviointityokaluKysymys = kysymys
                        )
                        vaihtoehto.teksti = vaihtoehtoDTO.teksti
                        vaihtoehto.valittu = vaihtoehtoDTO.valittu
                        vaihtoehto.arviointityokaluKysymys = kysymys
                        updatedVaihtoehdot.add(vaihtoehto)
                    }
                    kysymys.vaihtoehdot.retainAll(updatedVaihtoehdot.toSet())
                    kysymys.vaihtoehdot.addAll(updatedVaihtoehdot.filterNot { kysymys.vaihtoehdot.contains(it) })
                    updatedKysymykset.add(kysymys)
                }
                arviointityokalu.kysymykset.retainAll(updatedKysymykset.toSet())
                arviointityokalu.kysymykset.addAll(updatedKysymykset.filterNot { arviointityokalu.kysymykset.contains(it) })
                if (liiteData != null) {
                    arviointityokalu.liite = AsiakirjaData(data = liiteData.bytes)
                    arviointityokalu.liitetiedostonNimi = liiteData.originalFilename
                    arviointityokalu.liitetiedostonTyyppi = liiteData.contentType
                } else {
                    arviointityokalu.liite = null
                    arviointityokalu.liitetiedostonNimi = null
                    arviointityokalu.liitetiedostonTyyppi = null
                }
                arviointityokalu.muokkausaika = Instant.now()
                val result = arviointityokaluRepository.save(arviointityokalu)
                arviointityokaluMapper.toDto(result)
            }
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
            .map { arviointityokalu ->
                arviointityokalu.kysymykset = arviointityokalu.kysymykset
                    .sortedBy { it.jarjestysnumero }
                    .map { kysymys ->
                        kysymys.vaihtoehdot = kysymys.vaihtoehdot.sortedBy { it.id }.toMutableList()
                        kysymys
                    }.toMutableList()
                arviointityokaluMapper.toDto(arviointityokalu)
            }
    }

    override fun delete(id: Long) {
        arviointityokaluRepository.deleteById(id)
    }

}
