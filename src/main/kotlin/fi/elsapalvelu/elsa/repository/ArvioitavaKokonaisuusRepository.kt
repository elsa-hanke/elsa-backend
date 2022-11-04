package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArvioitavaKokonaisuus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ArvioitavaKokonaisuusRepository : JpaRepository<ArvioitavaKokonaisuus, Long> {

    @Query(
        "select oa from ArvioitavaKokonaisuus oa join oa.kategoria k left join k.erikoisala e " +
            "where e.id = ?1 " +
            "and oa.voimassaoloAlkaa <= ?2 " +
            "and (oa.voimassaoloLoppuu is null or oa.voimassaoloLoppuu >= ?2)"
    )
    fun findAllByErikoisalaIdAndValid(id: Long?, valid: LocalDate): List<ArvioitavaKokonaisuus>

    fun findAllByKategoriaErikoisalaIdIn(erikoisalaIds: List<Long>): List<ArvioitavaKokonaisuus>
}
