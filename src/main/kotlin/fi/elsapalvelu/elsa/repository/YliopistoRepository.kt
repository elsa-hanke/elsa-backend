package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Yliopisto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface YliopistoRepository : JpaRepository<Yliopisto, Long> {

    @Query(
        value = "select distinct yliopisto from Yliopisto yliopisto left join fetch yliopisto.erikoisalat",
        countQuery = "select count(distinct yliopisto) from Yliopisto yliopisto"
    )
    fun findAllWithEagerRelationships(pageable: Pageable): Page<Yliopisto>

    @Query("select distinct yliopisto from Yliopisto yliopisto left join fetch yliopisto.erikoisalat")
    fun findAllWithEagerRelationships(): MutableList<Yliopisto>

    @Query("select yliopisto from Yliopisto yliopisto left join fetch yliopisto.erikoisalat where yliopisto.id =:id")
    fun findOneWithEagerRelationships(@Param("id") id: Long): Optional<Yliopisto>
}
