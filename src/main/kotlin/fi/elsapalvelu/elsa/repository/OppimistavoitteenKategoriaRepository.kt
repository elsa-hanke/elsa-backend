package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OppimistavoitteenKategoriaRepository : JpaRepository<OppimistavoitteenKategoria, Long> {

    @Query(
        "select ok from OppimistavoitteenKategoria ok " +
            "left join ok.erikoisala e " +
            "left join fetch ok.oppimistavoitteet o " +
            "where e.id = ?1 " +
            "and o.kategoria.id = ok.id " +
            "and o.voimassaolonAlkamispaiva <= ?2 " +
            "and (o.voimassaolonPaattymispaiva is null or o.voimassaolonPaattymispaiva >= ?2) " +
            "and ok.voimassaolonAlkamispaiva <= ?2 " +
            "and (ok.voimassaolonPaattymispaiva is null or ok.voimassaolonPaattymispaiva >= ?2)"
    )
    fun findAllByErikoisalaIdAndValid(id: Long?, valid: LocalDate): List<OppimistavoitteenKategoria>

}
