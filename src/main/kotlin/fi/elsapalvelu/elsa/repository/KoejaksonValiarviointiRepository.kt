package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
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

    @Query("select v from KoejaksonValiarviointi v where v.erikoistuvaLaakari = (select k.erikoistuvaLaakari from KoejaksonKehittamistoimenpiteet k where k.id = :id)")
    fun findByKehittamistoimenpiteetId(id: Long): Optional<KoejaksonValiarviointi>

    @Query("select v from KoejaksonValiarviointi v where v.erikoistuvaLaakari = (select l.erikoistuvaLaakari from KoejaksonLoppukeskustelu l where l.id = :id)")
    fun findByLoppukeskusteluId(id: Long): Optional<KoejaksonValiarviointi>

    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        kouluttajaId: String,
        esimiesId: String
    ): List<KoejaksonValiarviointi>
}
