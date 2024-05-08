package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ValmistumispyynnonTarkistus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ValmistumispyynnonTarkistusRepository : JpaRepository<ValmistumispyynnonTarkistus, Long> {

    fun findByValmistumispyyntoId(id: Long): ValmistumispyynnonTarkistus?

    fun findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(
        id: Long,
        yliopistoId: Long
    ): ValmistumispyynnonTarkistus?

    @Query("""
        select t from ValmistumispyynnonTarkistus t join t.valmistumispyynto p join p.opintooikeus o
        where p.id = :id and o.yliopisto.id = :yliopistoId
    """)
    fun findByValmistumispyyntoIdForHyvaksyja(
        id: Long,
        yliopistoId: Long
    ): ValmistumispyynnonTarkistus?

}
