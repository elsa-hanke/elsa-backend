package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonKoulutussopimusRepository : JpaRepository<KoejaksonKoulutussopimus, Long> {
    fun findOneByIdAndKouluttajatKouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimus>
}
