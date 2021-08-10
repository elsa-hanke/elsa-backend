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

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointi>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointi>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonValiarviointi>

    @Query(
        "select v from KoejaksonValiarviointi v left join v.lahikouluttaja lk left join v.lahiesimies le " +
            "where lk.user.id = :userId or (le.user.id = :userId and (v.lahikouluttajaHyvaksynyt = true or v.korjausehdotus != null))"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        userId: String
    ): List<KoejaksonValiarviointi>

    @Transactional
    @Modifying
    @Query("update KoejaksonValiarviointi v set v.lahikouluttaja.id = :newKayttaja where v.lahikouluttaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)

    @Transactional
    @Modifying
    @Query("update KoejaksonValiarviointi v set v.lahiesimies.id = :newKayttaja where v.lahiesimies.id = :currentKayttaja")
    fun changeEsimies(currentKayttaja: Long, newKayttaja: Long)
}
