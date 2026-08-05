package fi.elsapalvelu.elsa.repository.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VerificationTokenRepository : JpaRepository<VerificationToken, String> {

    fun findOneByUserId(userId: String): VerificationToken?
}
