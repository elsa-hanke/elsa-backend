package fi.elsapalvelu.elsa.repository.valmistuminen

import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface ValmistumispyyntoRepository : JpaRepository<Valmistumispyynto, Long>,
    JpaSpecificationExecutor<Valmistumispyynto> {

    fun findByOpintooikeusId(opintooikeusId: Long): Valmistumispyynto?

    fun findByIdAndOpintooikeusYliopistoIdAndOpintooikeusErikoisalaIdIn(
        id: Long,
        yliopistoId: Long,
        erikoisalaIds: List<Long>
    ): Valmistumispyynto?

    fun findByIdAndOpintooikeusYliopistoId(
        id: Long,
        yliopistoId: Long
    ): Valmistumispyynto?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean

    /**
     * Loads the valmistumispyynto with a pessimistic write lock (`SELECT ... FOR UPDATE`) that is
     * held until the surrounding transaction ends. A lock timeout of 0 means the database does not
     * wait: if another transaction already holds the row, the call fails immediately with a
     * [org.springframework.dao.PessimisticLockingFailureException]. Must be called inside a
     * transaction, and before the entity is loaded by anything else in that transaction, so that
     * the returned state is the freshly locked one.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select v from Valmistumispyynto v where v.id = :id")
    fun findByIdForHyvaksynta(@Param("id") id: Long): Valmistumispyynto?

}
