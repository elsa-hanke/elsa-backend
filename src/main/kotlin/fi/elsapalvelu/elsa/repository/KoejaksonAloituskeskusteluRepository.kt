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

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonAloituskeskustelu>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonAloituskeskustelu>

    fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonAloituskeskustelu>

    @Query(
        "select a from KoejaksonAloituskeskustelu a left join a.lahikouluttaja lk left join a.lahiesimies le " +
            "where (lk.id = :kayttajaId or (le.id = :kayttajaId and (a.lahikouluttajaHyvaksynyt = true or a.korjausehdotus != null))) " +
            "and (a.lahetetty = true or a.korjausehdotus != null)"
    )
    fun findAllByLahikouluttajaIdOrLahiesimiesId(
        kayttajaId: String
    ): List<KoejaksonAloituskeskustelu>

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahikouluttaja.id = :newKayttaja where a.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: String, newKayttaja: String)

    @Transactional
    @Modifying
    @Query("update KoejaksonAloituskeskustelu a set a.lahiesimies.id = :newKayttaja where a.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: String, newKayttaja: String)
}
