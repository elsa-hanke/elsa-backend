package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonAloituskeskustelu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
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

    @Query("select a from KoejaksonAloituskeskustelu a where a.erikoistuvaLaakari = (select v.erikoistuvaLaakari from KoejaksonValiarviointi v where v.id = :id)")
    fun findByValiarviointiId(id: Long): Optional<KoejaksonAloituskeskustelu>

    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        kouluttajaId: String,
        esimiesId: String
    ): List<KoejaksonAloituskeskustelu>
}
