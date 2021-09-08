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

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskustelu>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskustelu>

    fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonLoppukeskustelu>

    @Query(
        "select l from KoejaksonLoppukeskustelu l left join l.lahikouluttaja lk left join l.lahiesimies le " +
            "where lk.id = :kayttajaId or (le.id = :kayttajaId and (l.lahikouluttajaHyvaksynyt = true or (l.korjausehdotus != null and l.korjausehdotus != '')))"
    )
    fun findAllByLahikouluttajaIdOrLahiesimiesId(
        kayttajaId: String
    ): List<KoejaksonLoppukeskustelu>

    @Transactional
    @Modifying
    @Query("update KoejaksonLoppukeskustelu l set l.lahikouluttaja.id = :newKayttaja where l.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: String, newKayttaja: String)

    @Transactional
    @Modifying
    @Query("update KoejaksonLoppukeskustelu l set l.lahiesimies.id = :newKayttaja where l.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: String, newKayttaja: String)
}
