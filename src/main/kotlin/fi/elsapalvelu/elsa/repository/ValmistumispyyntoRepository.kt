package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface ValmistumispyyntoRepository : JpaRepository<Valmistumispyynto, Long>,
    JpaSpecificationExecutor<Valmistumispyynto> {

    fun findByOpintooikeusId(opintooikeusId: Long): Valmistumispyynto?

    fun findByIdAndOpintooikeusYliopistoIdAndOpintooikeusErikoisalaIdIn(
        id: Long,
        yliopistoId: Long,
        erikoisalaIds: List<Long>
    ): Valmistumispyynto?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean

}
