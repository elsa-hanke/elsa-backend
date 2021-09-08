package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKehittamistoimenpiteet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface KoejaksonKehittamistoimenpiteetRepository :
    JpaRepository<KoejaksonKehittamistoimenpiteet, Long> {

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteet>

    fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonKehittamistoimenpiteet>

    @Query(
        "select k from KoejaksonKehittamistoimenpiteet k left join k.lahikouluttaja lk left join k.lahiesimies le " +
            "where lk.id = :kayttajaId or (le.id = :kayttajaId and (k.lahikouluttajaHyvaksynyt = true or (k.korjausehdotus != null and k.korjausehdotus != '')))"
    )
    fun findAllByLahikouluttajaIdOrLahiesimiesId(
        kayttajaId: String
    ): List<KoejaksonKehittamistoimenpiteet>

    @Transactional
    @Modifying
    @Query("update KoejaksonKehittamistoimenpiteet k set k.lahikouluttaja.id = :newKayttaja where k.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: String, newKayttaja: String)

    @Transactional
    @Modifying
    @Query("update KoejaksonKehittamistoimenpiteet k set k.lahiesimies.id = :newKayttaja where k.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: String, newKayttaja: String)
}
