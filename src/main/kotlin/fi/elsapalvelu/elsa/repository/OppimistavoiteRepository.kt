package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Oppimistavoite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OppimistavoiteRepository : JpaRepository<Oppimistavoite, Long> {

    @Query(
        "select o from Oppimistavoite o " +
            "where o.voimassaolonAlkamispaiva <= ?1 " +
            "and (o.voimassaolonPaattymispaiva is null or o.voimassaolonPaattymispaiva >= ?1)"
    )
    fun findAllByValid(valid: LocalDate): List<Oppimistavoite>

}
