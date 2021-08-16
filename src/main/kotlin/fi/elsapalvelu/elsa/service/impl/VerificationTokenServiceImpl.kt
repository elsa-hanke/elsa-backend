package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.VerificationToken
import fi.elsapalvelu.elsa.repository.VerificationTokenRepository
import fi.elsapalvelu.elsa.service.VerificationTokenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VerificationTokenServiceImpl(
    private val verificationTokenRepository: VerificationTokenRepository
) : VerificationTokenService {

    override fun save(userId: String): String {
        val verificationToken = VerificationToken(user = User(id = userId))
        return verificationTokenRepository.save(verificationToken).id!!
    }
}
