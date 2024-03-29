package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SuoritteenKategoriaRepository : JpaRepository<SuoritteenKategoria, Long> {

    // Haetaan kategoriat, jotka ovat voimassa mikäli kategorian alta löytyy yksi tai useampi
    // suorite, joka on voimassa.
    @Query(
        "select distinct sk from SuoritteenKategoria sk " +
            "left join sk.erikoisala e " +
            "left join fetch sk.suoritteet s " +
            "where e.id = ?1 " +
            "and s.kategoria.id = sk.id " +
            "and s.voimassaolonAlkamispaiva <= ?2 " +
            "and (s.voimassaolonPaattymispaiva is null or s.voimassaolonPaattymispaiva >= ?2)"
    )
    fun findAllByErikoisalaIdAndValid(id: Long?, valid: LocalDate): List<SuoritteenKategoria>

    @Query(
        "select distinct sk from SuoritteenKategoria sk " +
            "left join sk.erikoisala e " +
            "left join fetch sk.suoritteet s " +
            "where e.id = ?1 " +
            "and s.kategoria.id = sk.id " +
            "and s.voimassaolonPaattymispaiva < ?2"
    )
    fun findAllByErikoisalaIdAndExpired(id: Long?, valid: LocalDate): List<SuoritteenKategoria>

    @Query(
        """
    select distinct sk from SuoritteenKategoria sk
    left join fetch sk.suoritteet s
    where sk.erikoisala.id = ?1
    """
    )
    fun findAllByErikoisalaId(erikoisalaId: Long): List<SuoritteenKategoria>

}
