package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Kayttaja
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

    override fun save(kayttajaId: String): String {
        val verificationToken = VerificationToken(kayttaja = Kayttaja(id = kayttajaId))
        return verificationTokenRepository.save(verificationToken).id!!
    }
}
