package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
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

    override fun findOne(userId: String): String? {
        return verificationTokenRepository.findOneByUserId(userId)?.id
    }
}
