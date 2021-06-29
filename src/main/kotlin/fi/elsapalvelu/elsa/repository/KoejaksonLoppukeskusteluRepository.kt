package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
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
            "where lk.user.id = :userId or (le.user.id = :userId and l.lahikouluttajaHyvaksynyt = true)"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonLoppukeskustelu>

    @Transactional
    @Modifying
    @Query("update KoejaksonLoppukeskustelu l set l.lahikouluttaja.id = :newKayttaja where l.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonLoppukeskustelu l set l.lahiesimies.id = :newKayttaja where l.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: Long, newKayttaja: Long)
}
