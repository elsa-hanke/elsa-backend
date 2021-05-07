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
        "select k from KoejaksonKehittamistoimenpiteet k " +
            "where k.erikoistuvaLaakari = (" +
            "select l.erikoistuvaLaakari from KoejaksonLoppukeskustelu l where l.id = :id)"
    )
    fun findByLoppukeskusteluId(id: Long): Optional<KoejaksonKehittamistoimenpiteet>

    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        kouluttajaId: String,
        esimiesId: String
    ): List<KoejaksonKehittamistoimenpiteet>
}
