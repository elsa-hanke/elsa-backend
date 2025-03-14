package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Arviointityokalu
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
            arviointityokalu.versio = 1
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
        arviointityokalu.alkuperainenId = arviointityokalu.id!!
        arviointityokalu = arviointityokaluRepository.save(arviointityokalu)
        return arviointityokaluMapper.toDto(arviointityokalu)
    }

    override fun update(
        arviointityokaluDTO: ArviointityokaluDTO,
        liiteData: MultipartFile?
    ): ArviointityokaluDTO? {
        return arviointityokaluRepository.findById(arviointityokaluDTO.id!!)
            .orElse(null)?.let { vanhaArviointityokalu ->
                vanhaArviointityokalu.kaytossa = false
                arviointityokaluRepository.save(vanhaArviointityokalu)
                val now = Instant.now()
                val uusiArviointityokalu = Arviointityokalu(
                    id = null,
                    alkuperainenId = vanhaArviointityokalu.alkuperainenId ?: vanhaArviointityokalu.id!!,
                    versio = vanhaArviointityokalu.versio + 1,
                    nimi = arviointityokaluDTO.nimi,
                    ohjeteksti = arviointityokaluDTO.ohjeteksti,
                    tila = arviointityokaluDTO.tila,
                    kaytossa = true,
                    luontiaika = vanhaArviointityokalu.luontiaika,
                    muokkausaika = now,
                    kayttaja = null,
                    kategoria = arviointityokaluDTO.kategoria?.let {
                        arviointityokaluKategoriaRepository.findById(it.id!!).orElse(null)
                    }
                )
                val updatedKysymykset = arviointityokaluDTO.kysymykset?.map { kysymysDTO ->
                    val kysymys = ArviointityokaluKysymys(
                        id = null,
                        arviointityokalu = uusiArviointityokalu,
                        otsikko = kysymysDTO.otsikko,
                        pakollinen = kysymysDTO.pakollinen,
                        jarjestysnumero = kysymysDTO.jarjestysnumero,
                        tyyppi = kysymysDTO.tyyppi
                    )
                    kysymys.vaihtoehdot = kysymysDTO.vaihtoehdot?.map { vaihtoehtoDTO ->
                        ArviointityokaluKysymysVaihtoehto(
                            id = null,
                            teksti = vaihtoehtoDTO.teksti,
                            arviointityokaluKysymys = kysymys
                        )
                    }?.toMutableList() ?: mutableListOf()
                    kysymys
                }?.toMutableList() ?: mutableListOf()

                uusiArviointityokalu.kysymykset = updatedKysymykset

                if (liiteData != null) {
                    uusiArviointityokalu.liite = AsiakirjaData(data = liiteData.bytes)
                    uusiArviointityokalu.liitetiedostonNimi = liiteData.originalFilename
                    uusiArviointityokalu.liitetiedostonTyyppi = liiteData.contentType
                } else {
                    uusiArviointityokalu.liite = null
                    uusiArviointityokalu.liitetiedostonNimi = null
                    uusiArviointityokalu.liitetiedostonTyyppi = null
                }

                val savedArviointityokalu = arviointityokaluRepository.save(uusiArviointityokalu)
                return arviointityokaluMapper.toDto(savedArviointityokalu)
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
        arviointityokaluRepository.findById(id).ifPresent { arviointityokalu ->
            arviointityokalu.kaytossa = false
            arviointityokaluRepository.save(arviointityokalu)
        }
    }

}
