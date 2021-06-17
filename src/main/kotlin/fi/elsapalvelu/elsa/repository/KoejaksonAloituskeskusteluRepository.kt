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

    @Query(
        "select a " +
            "from KoejaksonAloituskeskustelu a left join a.lahikouluttaja lk left join a.lahiesimies le " +
            "where (lk.user.id = :kouluttajaId or le.user.id = :esimiesId) and (a.lahetetty = true or a.korjausehdotus != null)"
    )
    fun findAllByLahikouluttajaUserIdOrLahiesimiesUserId(
        kouluttajaId: String,
        esimiesId: String
    ): List<KoejaksonAloituskeskustelu>
}
