package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonValiarviointiRepository : JpaRepository<KoejaksonValiarviointi, Long> {

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointi>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointi>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonValiarviointi>

    @Query(
        "select v from KoejaksonValiarviointi v left join v.lahikouluttaja lk left join v.lahiesimies le " +
            "where lk.user.id = :userId or (le.user.id = :userId and v.lahikouluttajaHyvaksynyt = true)"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonValiarviointi>
}
