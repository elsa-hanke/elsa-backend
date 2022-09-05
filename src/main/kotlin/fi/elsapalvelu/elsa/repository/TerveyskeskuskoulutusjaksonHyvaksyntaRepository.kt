package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TerveyskeskuskoulutusjaksonHyvaksyntaRepository :
    JpaRepository<TerveyskeskuskoulutusjaksonHyvaksynta, Long> {

    fun findByOpintooikeusId(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksynta?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean
}
