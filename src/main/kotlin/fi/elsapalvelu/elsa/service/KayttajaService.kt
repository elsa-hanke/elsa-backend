package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaWrapperDTO
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

    fun findKouluttajatFromSameYliopisto(userId: String): List<KayttajaDTO>

    fun findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
        userId: String,
        tehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum
    ): KayttajaDTO

    fun findVastuuhenkilotByYliopisto(
        yliopistoId: Long
    ): List<KayttajaDTO>

    fun findTeknisetPaakayttajat(): List<KayttajaDTO>

    fun findKouluttajatAndVastuuhenkilotFromSameYliopisto(userId: String): List<KayttajaDTO>

    fun delete(id: Long)

    fun updateKouluttajaYliopistoAndErikoisalaByEmail(
        erikoistuvaUserId: String,
        kouluttajaEmail: String
    ): KayttajaDTO?

    fun findVastuuhenkilotFromSameYliopistoAndErikoisala(kayttajaId: Long): List<KayttajaDTO>

    fun findByKayttajahallintaCriteriaFromSameYliopisto(
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

    fun saveVastuuhenkilo(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        kayttajaId: Long? = null
    ): KayttajahallintaKayttajaWrapperDTO

    fun saveKayttajahallintaKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        authorities: Set<String>? = null,
        kayttajaId: Long? = null
    ): KayttajahallintaKayttajaWrapperDTO
}
