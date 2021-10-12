package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Koulutusjakso
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface KoulutusjaksoRepository : JpaRepository<Koulutusjakso, Long> {

    @Query(
        value = "select distinct koulutusjakso " +
            "from Koulutusjakso koulutusjakso " +
            "left join fetch koulutusjakso.tyoskentelyjaksot " +
            "left join fetch koulutusjakso.osaamistavoitteet",
        countQuery = "select count(distinct koulutusjakso) from Koulutusjakso koulutusjakso"
    )
    fun findAllWithEagerRelationships(pageable: Pageable): Page<Koulutusjakso>

    @Query(
        "select distinct koulutusjakso " +
            "from Koulutusjakso koulutusjakso " +
            "left join fetch koulutusjakso.tyoskentelyjaksot " +
            "left join fetch koulutusjakso.osaamistavoitteet"
    )
    fun findAllWithEagerRelationships(): MutableList<Koulutusjakso>

    @Query(
        "select koulutusjakso " +
            "from Koulutusjakso koulutusjakso " +
            "left join fetch koulutusjakso.tyoskentelyjaksot " +
            "left join fetch koulutusjakso.osaamistavoitteet " +
            "where koulutusjakso.id =:id"
    )
    fun findOneWithEagerRelationships(@Param("id") id: Long): Koulutusjakso?

    fun findAllByKoulutussuunnitelmaErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<Koulutusjakso>

    fun findOneByIdAndKoulutussuunnitelmaErikoistuvaLaakariKayttajaUserId(id: Long, userId: String): Koulutusjakso?

    fun deleteByIdAndKoulutussuunnitelmaErikoistuvaLaakariKayttajaUserId(id: Long, userId: String)
}
