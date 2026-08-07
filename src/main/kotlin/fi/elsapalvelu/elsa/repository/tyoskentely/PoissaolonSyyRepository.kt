package fi.elsapalvelu.elsa.repository.tyoskentely

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PoissaolonSyyRepository : JpaRepository<PoissaolonSyy, Long> {

    @Query(
        "select p from PoissaolonSyy p " +
            "where p.voimassaolonAlkamispaiva <= ?1 " +
            "and (p.voimassaolonPaattymispaiva is null or p.voimassaolonPaattymispaiva >= ?1)"
    )
    fun findAllByValid(valid: LocalDate): List<PoissaolonSyy>

}
