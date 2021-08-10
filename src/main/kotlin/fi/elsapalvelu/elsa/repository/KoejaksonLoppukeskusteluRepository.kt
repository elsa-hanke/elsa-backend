package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonLoppukeskusteluRepository : JpaRepository<KoejaksonLoppukeskustelu, Long> {

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonLoppukeskustelu>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonLoppukeskustelu>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonLoppukeskustelu>

    @Query(
        "select l from KoejaksonLoppukeskustelu l left join l.lahikouluttaja lk left join l.lahiesimies le " +
            "where lk.user.id = :userId or (le.user.id = :userId and (l.lahikouluttajaHyvaksynyt = true or l.korjausehdotus != null))"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonLoppukeskustelu>
}
