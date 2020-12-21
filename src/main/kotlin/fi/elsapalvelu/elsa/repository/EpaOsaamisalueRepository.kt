package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EpaOsaamisalueRepository : JpaRepository<EpaOsaamisalue, Long> {

    @Query("select oa from EpaOsaamisalue oa where oa.voimassaoloAlkaa <= ?1 and (oa.voimassaoloLoppuu is null or oa.voimassaoloLoppuu >= ?1)")
    fun findAllValid(now: LocalDate): List<EpaOsaamisalue>
}
