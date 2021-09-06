 package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Erikoisala
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

 @Repository
 interface ErikoisalaRepository : JpaRepository<Erikoisala, Long> {

    @Query(
        "select e from Erikoisala e " +
            "where e.voimassaoloAlkaa <= ?1 " +
            "and (e.voimassaoloPaattyy is null or e.voimassaoloPaattyy >= ?1)"
    )
    fun findAllByValid(valid: LocalDate): List<Erikoisala>
}
