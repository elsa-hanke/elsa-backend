package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TerveyskeskuskoulutusjaksonHyvaksyntaRepository :
    JpaRepository<TerveyskeskuskoulutusjaksonHyvaksynta, Long>,
    JpaSpecificationExecutor<TerveyskeskuskoulutusjaksonHyvaksynta> {

    fun findByIdAndOpintooikeusYliopistoIdIn(
        id: Long,
        yliopistoIds: List<Long>
    ): TerveyskeskuskoulutusjaksonHyvaksynta?

    @Query(
        """
        select t from TerveyskeskuskoulutusjaksonHyvaksynta t
        join t.opintooikeus o
        where t.id = :id and o.yliopisto.id in :yliopistoIds and (t.virkailijaHyvaksynyt = true or t.korjausehdotusVastuuhenkilolta is not null)
        """
    )
    fun findByIdAndYliopistoIdForVastuuhenkilo(
        id: Long,
        yliopistoIds: List<Long>
    ): TerveyskeskuskoulutusjaksonHyvaksynta?

    fun findByOpintooikeusId(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksynta?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean
}
