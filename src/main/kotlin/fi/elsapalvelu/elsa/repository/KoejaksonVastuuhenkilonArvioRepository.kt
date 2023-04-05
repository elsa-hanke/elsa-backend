package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonVastuuhenkilonArvioRepository :
    JpaRepository<KoejaksonVastuuhenkilonArvio, Long>,
    JpaSpecificationExecutor<KoejaksonVastuuhenkilonArvio> {

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvio>

    fun existsByIdAndVastuuhenkiloUserId(id: Long, userId: String): Boolean

    fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonVastuuhenkilonArvio>

    @Query(
        """
        select a
        from KoejaksonVastuuhenkilonArvio a join a.vastuuhenkilo v
        where v.user.id = :userId and (a.virkailijaHyvaksynyt = true or a.vastuuhenkilonKorjausehdotus is not null)
        """
    )
    fun findAllByVastuuhenkiloUserIdAndVirkailijaHyvaksynytTrue(
        userId: String
    ): List<KoejaksonVastuuhenkilonArvio>

    @Query(
        """
        select a
        from KoejaksonVastuuhenkilonArvio a join a.vastuuhenkilo v
        where v.user.id = :userId and a.virkailijaHyvaksynyt = true and a.vastuuhenkiloHyvaksynyt = false
        """
    )
    fun findAllAvoinByVastuuhenkilo(
        userId: String
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
