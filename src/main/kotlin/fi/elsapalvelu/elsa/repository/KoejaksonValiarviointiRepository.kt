package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface KoejaksonValiarviointiRepository : JpaRepository<KoejaksonValiarviointi, Long> {

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonValiarviointi>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonValiarviointi>

    fun findByErikoistuvaLaakariKayttajaId(kayttajaId: String): Optional<KoejaksonValiarviointi>

    @Query(
        "select v from KoejaksonValiarviointi v left join v.lahikouluttaja lk left join v.lahiesimies le " +
            "where lk.id = :kayttajaId or (le.id = :kayttajaId and (v.lahikouluttajaHyvaksynyt = true or (v.korjausehdotus != null and v.korjausehdotus != '')))"
    )
    fun findAllByLahikouluttajaIdOrLahiesimiesId(
        kayttajaId: String
    ): List<KoejaksonValiarviointi>

    @Transactional
    @Modifying
    @Query("update KoejaksonValiarviointi v set v.lahikouluttaja.id = :newKayttaja where v.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: String, newKayttaja: String)

    @Transactional
    @Modifying
    @Query("update KoejaksonValiarviointi v set v.lahiesimies.id = :newKayttaja where v.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: String, newKayttaja: String)
}
