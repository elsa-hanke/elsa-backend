package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.Yliopisto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Spring Data  repository for the [Yliopisto] entity.
 */
@Repository
interface YliopistoRepository : JpaRepository<Yliopisto, Long> {

    @Query(
        value = "select distinct yliopisto from Yliopisto yliopisto left join fetch yliopisto.erikoisalas",
        countQuery = "select count(distinct yliopisto) from Yliopisto yliopisto"
    )
    fun findAllWithEagerRelationships(pageable: Pageable): Page<Yliopisto>

    @Query("select distinct yliopisto from Yliopisto yliopisto left join fetch yliopisto.erikoisalas")
    fun findAllWithEagerRelationships(): MutableList<Yliopisto>

    @Query("select yliopisto from Yliopisto yliopisto left join fetch yliopisto.erikoisalas where yliopisto.id =:id")
    fun findOneWithEagerRelationships(@Param("id") id: Long): Optional<Yliopisto>
}
