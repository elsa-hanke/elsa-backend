package fi.elsapalvelu.elsa.repository.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KayttajaYliopistoErikoisalaRepository : JpaRepository<KayttajaYliopistoErikoisala, Long> {

    fun findAllByYliopistoIdAndErikoisalaId(yliopistoId: Long, erikoisalaId: Long): List<KayttajaYliopistoErikoisala>

    fun findAllByYliopistoIdAndErikoisalaIdAndKayttajaId(yliopistoId: Long, erikoisalaId: Long, kayttajaId: Long): List<KayttajaYliopistoErikoisala>

}
