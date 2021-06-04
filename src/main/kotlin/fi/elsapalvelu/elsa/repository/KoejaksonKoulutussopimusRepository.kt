package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonKoulutussopimusRepository : JpaRepository<KoejaksonKoulutussopimus, Long> {

    fun findOneByIdAndKouluttajatKouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimus>

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimus>

    fun findByErikoistuvaLaakariKayttajaUserId(userId: String): Optional<KoejaksonKoulutussopimus>

    @Query(
        "select ks " +
            "from KoejaksonKoulutussopimus ks join ks.kouluttajat ko join ko.kouluttaja k " +
            "where k.user.id = :userId and (ks.lahetetty = true or ks.korjausehdotus != null)"
    )
    fun findAllByKouluttajatKouluttajaUserId(
        userId: String
    ): List<KoejaksonKoulutussopimus>
}
