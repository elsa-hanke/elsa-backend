package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
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

    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        kouluttajaId: String,
        esimiesId: String
    ): List<KoejaksonLoppukeskustelu>
}
