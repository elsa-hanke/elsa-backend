package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ValmistumispyynnonTarkistus
import org.springframework.data.jpa.repository.JpaRepository

interface ValmistumispyynnonTarkistusRepository : JpaRepository<ValmistumispyynnonTarkistus, Long> {

    fun findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(
        id: Long,
        yliopistoId: Long
    ): ValmistumispyynnonTarkistus?

}
