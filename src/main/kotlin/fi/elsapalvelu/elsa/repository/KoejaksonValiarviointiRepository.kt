package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
import org.springframework.data.jpa.repository.JpaRepository
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

    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        kouluttajaId: String,
        esimiesId: String
    ): List<KoejaksonValiarviointi>
}
