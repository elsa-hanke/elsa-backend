package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suorite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SuoriteRepository : JpaRepository<Suorite, Long> {

    @Query(
        "select s from Suorite s " +
            "where s.voimassaolonAlkamispaiva <= ?1 " +
            "and (s.voimassaolonPaattymispaiva is null or s.voimassaolonPaattymispaiva >= ?1)"
    )
    fun findAllByValid(valid: LocalDate): List<Suorite>

}
