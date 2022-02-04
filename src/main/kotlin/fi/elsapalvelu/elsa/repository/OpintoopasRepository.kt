package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintoopas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface OpintoopasRepository : JpaRepository<Opintoopas, Long> {

    fun findAllByOpintooikeudetErikoistuvaLaakariKayttajaUserId(userId: String): List<Opintoopas>

}
