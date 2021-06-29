package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonAloituskeskustelu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface KoejaksonAloituskeskusteluRepository : JpaRepository<KoejaksonAloituskeskustelu, Long> {

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskustelu>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskustelu>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonAloituskeskustelu>

    @Query(
        "select a from KoejaksonAloituskeskustelu a left join a.lahikouluttaja lk left join a.lahiesimies le " +
            "where (lk.user.id = :userId or (le.user.id = :userId and a.lahikouluttajaHyvaksynyt = true)) " +
            "and (a.lahetetty = true or a.korjausehdotus != null)"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonAloituskeskustelu>

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahikouluttaja.id = :newKayttaja where a.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahiesimies.id = :newKayttaja where a.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: Long, newKayttaja: Long)
}
