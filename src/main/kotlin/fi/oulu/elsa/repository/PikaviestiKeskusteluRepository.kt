package fi.oulu.elsa.repository

import fi.oulu.elsa.domain.PikaviestiKeskustelu
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Spring Data  repository for the [PikaviestiKeskustelu] entity.
 */
@Repository
interface PikaviestiKeskusteluRepository : JpaRepository<PikaviestiKeskustelu, Long> {

    @Query(
        value = "select distinct pikaviestiKeskustelu from PikaviestiKeskustelu pikaviestiKeskustelu" +
            " left join fetch pikaviestiKeskustelu.keskustelijas",
        countQuery = "select count(distinct pikaviestiKeskustelu) from PikaviestiKeskustelu pikaviestiKeskustelu"
    )
    fun findAllWithEagerRelationships(pageable: Pageable): Page<PikaviestiKeskustelu>

    @Query(
        "select distinct pikaviestiKeskustelu from PikaviestiKeskustelu pikaviestiKeskustelu" +
            " left join fetch pikaviestiKeskustelu.keskustelijas"
    )
    fun findAllWithEagerRelationships(): MutableList<PikaviestiKeskustelu>

    @Query(
        "select pikaviestiKeskustelu from PikaviestiKeskustelu pikaviestiKeskustelu" +
            " left join fetch pikaviestiKeskustelu.keskustelijas where pikaviestiKeskustelu.id =:id"
    )
    fun findOneWithEagerRelationships(@Param("id") id: Long): Optional<PikaviestiKeskustelu>
}
