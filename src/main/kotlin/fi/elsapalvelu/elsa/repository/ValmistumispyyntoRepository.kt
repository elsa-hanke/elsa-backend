package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import org.springframework.data.jpa.repository.JpaRepository

interface ValmistumispyyntoRepository : JpaRepository<Valmistumispyynto, Long> {

    fun findByOpintooikeusId(opintooikeusId: Long): Valmistumispyynto?
}
