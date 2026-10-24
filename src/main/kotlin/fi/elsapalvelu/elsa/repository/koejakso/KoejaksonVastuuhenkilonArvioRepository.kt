package fi.elsapalvelu.elsa.repository.koejakso

import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonVastuuhenkilonArvioRepository :
    JpaRepository<KoejaksonVastuuhenkilonArvio, Long>,
    JpaSpecificationExecutor<KoejaksonVastuuhenkilonArvio> {

    /** Call before loading the assessment in the write transaction; serializes all updates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from KoejaksonVastuuhenkilonArvio a where a.id = :id")
    fun findByIdForUpdate(id: Long): KoejaksonVastuuhenkilonArvio?

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvio>

    fun existsByIdAndVastuuhenkiloUserId(id: Long, userId: String): Boolean

    fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonVastuuhenkilonArvio>

    @Query(
        """
        select a
        from KoejaksonVastuuhenkilonArvio a join a.opintooikeus o
        where o.yliopisto.id = :yliopistoId and o.erikoisala.id = :erikoisalaId and (a.virkailijaHyvaksynyt = true or a.vastuuhenkilonKorjausehdotus is not null)
        """
    )
    fun findAllByVastuuhenkilo(
        yliopistoId: Long,
        erikoisalaId: Long
    ): List<KoejaksonVastuuhenkilonArvio>

    @Query(
        """
        select a
        from KoejaksonVastuuhenkilonArvio a join a.opintooikeus o
        where o.yliopisto.id = :yliopistoId and o.erikoisala.id = :erikoisalaId and a.virkailijaHyvaksynyt = true and a.vastuuhenkiloHyvaksynyt = false
        """
    )
    fun findAllAvoinByVastuuhenkilo(
        yliopistoId: Long,
        erikoisalaId: Long
    ): List<KoejaksonVastuuhenkilonArvio>

    @Query(
        """
        select a
        from KoejaksonVastuuhenkilonArvio a join a.opintooikeus o
        where o.yliopisto.id = :yliopistoId and a.erikoistuvanKuittausaika is not null and a.virkailijaHyvaksynyt = false
        """
    )
    fun findAllAvoinByVirkailija(
        yliopistoId: Long
    ): List<KoejaksonVastuuhenkilonArvio>

    fun findOneByIdAndOpintooikeusYliopistoId(
        id: Long,
        yliopistoId: Long
    ): Optional<KoejaksonVastuuhenkilonArvio>
}
