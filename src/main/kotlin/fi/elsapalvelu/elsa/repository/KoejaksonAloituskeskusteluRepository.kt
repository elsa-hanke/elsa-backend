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

    fun findOneByIdAndLahiesimiesHyvaksynytTrue(id: Long): Optional<KoejaksonAloituskeskustelu>

    fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonAloituskeskustelu>

    @Query(
        "select a from KoejaksonAloituskeskustelu a left join a.lahikouluttaja lk left join a.lahiesimies le " +
            "where (lk.user.id = :userId or (le.user.id = :userId and (a.lahikouluttajaHyvaksynyt = true or a.korjausehdotus != null))) " +
            "and (a.lahetetty = true or a.korjausehdotus != null)"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonAloituskeskustelu>

    @Query(
        "select a from KoejaksonAloituskeskustelu a left join a.lahikouluttaja lk left join a.lahiesimies le " +
            "where (lk.user.id = :userId and a.lahetetty = true and a.lahikouluttajaHyvaksynyt = false) or (le.user.id = :userId and a.lahikouluttajaHyvaksynyt = true and a.lahiesimiesHyvaksynyt = false)"
    )
    fun findAllAvoinByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonAloituskeskustelu>

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahikouluttaja.id = :newKayttaja where a.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahikouluttaja.id = :newKayttaja where a.lahikouluttaja.id = :currentKayttaja and a.lahiesimiesHyvaksynyt = false")
    fun changeAvoinKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahiesimies.id = :newKayttaja where a.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahiesimies.id = :newKayttaja where a.lahiesimies.id = :currentKayttaja and a.lahiesimiesHyvaksynyt = false")
    fun changeAvoinEsimies(currentKayttaja: Long, newKayttaja: Long)

    @Query(
        """
        select case when count(k)> 0 then true else false end
        from KoejaksonAloituskeskustelu k
        where k.lahiesimiesHyvaksynyt = false and (k.lahikouluttaja.id = :kouluttajaId or k.lahiesimies.id = :kouluttajaId)
        """
    )
    fun existsAvoinForKouluttaja(kouluttajaId: Long): Boolean
}
