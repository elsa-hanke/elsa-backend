package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Opintoopas
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface OpintoopasRepository : JpaRepository<Opintoopas, Long> {

    fun findAllByOpintooikeudetErikoistuvaLaakariKayttajaUserId(userId: String): List<Opintoopas>

    @Query(
        "select o from Opintoopas o " +
            "where o.voimassaoloAlkaa <= ?1 " +
            "and (o.voimassaoloPaattyy is null or o.voimassaoloPaattyy >= ?1)"
    )
    fun findAllByValid(valid: LocalDate): List<Opintoopas>
}
