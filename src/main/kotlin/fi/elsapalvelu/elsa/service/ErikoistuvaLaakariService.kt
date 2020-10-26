package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import java.util.Optional

interface ErikoistuvaLaakariService {

    fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    fun findAll(): MutableList<ErikoistuvaLaakariDTO>

    fun findAllWhereHopsIsNull(): MutableList<ErikoistuvaLaakariDTO>

    fun findAllWhereKoejaksoIsNull(): MutableList<ErikoistuvaLaakariDTO>

    fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO>

    fun delete(id: Long)

    fun findOneByKayttajaUserId(id: String): Optional<ErikoistuvaLaakariDTO>
}
