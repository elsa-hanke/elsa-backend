package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.service.kayttaja.VerificationTokenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VerificationTokenServiceImpl(
    private val verificationTokenRepository: VerificationTokenRepository
) : VerificationTokenService {

    override fun save(userId: String): String {
        // Poistetaan mahdollinen olemassaoleva token, jotta ei rikota
        // ux_verification_token_user_id -uniikkirajoitetta, jos käyttäjälle
        // yritetään luoda uusi token ennen kuin edellistä on käytetty/poistettu.
        verificationTokenRepository.findOneByUserId(userId)?.let {
            verificationTokenRepository.delete(it)
            verificationTokenRepository.flush()
        }
        val verificationToken = VerificationToken(user = User(id = userId))
        return verificationTokenRepository.save(verificationToken).id.required()
    }

    override fun findOne(userId: String): String? {
        return verificationTokenRepository.findOneByUserId(userId)?.id
    }
}
