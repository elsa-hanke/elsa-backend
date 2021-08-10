package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKehittamistoimenpiteet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonKehittamistoimenpiteetRepository :
    JpaRepository<KoejaksonKehittamistoimenpiteet, Long> {

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonKehittamistoimenpiteet>

    @Query(
        "select k from KoejaksonKehittamistoimenpiteet k left join k.lahikouluttaja lk left join k.lahiesimies le " +
            "where lk.user.id = :userId or (le.user.id = :userId and (k.lahikouluttajaHyvaksynyt = true or k.korjausehdotus != null))"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonKehittamistoimenpiteet>
}
