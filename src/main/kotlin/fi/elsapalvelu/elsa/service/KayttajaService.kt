package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface KayttajaService {

    fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO

    fun saveKouluttaja(
        erikoistuvaUserId: String,
        kayttajaDTO: KayttajaDTO,
        userDTO: UserDTO
    ): KayttajaDTO

    fun findAll(): List<KayttajaDTO>

    fun findOne(id: Long): Optional<KayttajaDTO>

    fun findByUserId(id: String): Optional<KayttajaDTO>

    fun findKouluttajatUnderSameYliopisto(userId: String): List<KayttajaDTO>

    fun findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
        userId: String,
        tehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum
    ): KayttajaDTO

    fun findTeknisetPaakayttajat(): List<KayttajaDTO>

    fun findKouluttajatAndVastuuhenkilotUnderSameYliopisto(userId: String): List<KayttajaDTO>

    fun delete(id: Long)

    fun updateKouluttajaYliopistoAndErikoisalaByEmail(
        erikoistuvaUserId: String,
        kouluttajaEmail: String
    ): KayttajaDTO?

    fun findVastuuhenkilotUnderSameYliopisto(userId: String): List<KayttajaDTO>

    fun findByKayttajahallintaCriteriaUnderSameYliopisto(
        userId: String,
        authority: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO>

    fun findByKayttajahallintaCriteria(
        userId: String,
        authority: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO>

    fun activateKayttaja(kayttajaId: Long)

    fun passivateKayttaja(kayttajaId: Long)
}
