package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EpaOsaamisalueRepository : JpaRepository<EpaOsaamisalue, Long> {

    @Query(
        "select oa from EpaOsaamisalue oa left join oa.erikoisala e " +
            "where e.id = ?1 " +
            "and oa.voimassaoloAlkaa <= ?2 " +
            "and (oa.voimassaoloLoppuu is null or oa.voimassaoloLoppuu >= ?2)"
    )
    fun findAllByErikoisalaIdAndValid(id: Long?, valid: LocalDate): List<EpaOsaamisalue>
}
