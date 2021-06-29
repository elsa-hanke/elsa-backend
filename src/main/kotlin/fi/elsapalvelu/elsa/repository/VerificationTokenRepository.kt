package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.VerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VerificationTokenRepository : JpaRepository<VerificationToken, String>
