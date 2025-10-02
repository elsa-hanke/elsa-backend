package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.AsiakirjaData
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDataDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.springframework.web.multipart.MultipartFile
import java.util.*

interface ArviointityokaluService {

    fun save(
        arviointityokaluDTO: ArviointityokaluDTO,
        user: UserDTO,
        liiteData: MultipartFile?
    ): ArviointityokaluDTO

    fun update(
        arviointityokaluDTO: ArviointityokaluDTO,
        liiteData: MultipartFile?
    ): ArviointityokaluDTO?

    fun findAll(): List<ArviointityokaluDTO>

    fun findAllJulkaistut(): List<ArviointityokaluDTO>

    fun findAllJulkaistu(): List<ArviointityokaluDTO>

    fun findAllByKayttajaUserId(userId: String): MutableList<ArviointityokaluDTO>

    fun findOne(id: Long): Optional<ArviointityokaluDTO>

    fun delete(id: Long)

    fun findAllPoistetut(): List<ArviointityokaluDTO>

    fun palauta(id: Long)

    fun findOneByLiiteId(id: Long): ArviointityokaluDTO

    fun getAsiakirjaDataDTO(asiakirjaData: AsiakirjaData?): AsiakirjaDataDTO

}
