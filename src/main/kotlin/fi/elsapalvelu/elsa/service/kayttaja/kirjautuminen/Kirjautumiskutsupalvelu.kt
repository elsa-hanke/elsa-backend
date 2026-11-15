package fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen

import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.security.LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE
import fi.elsapalvelu.elsa.security.LoginNameMismatch
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import javax.crypto.Cipher
import javax.crypto.SecretKey
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/** SAML-pyynnon oletusarvo, joka ei ole kutsutunniste. */
internal const val KIRJAUTUMISEN_OLETUS_RELAY_STATE = "elsa"

@Service
class Kirjautumiskutsupalvelu(
    private val kutsut: VerificationTokenRepository,
    private val kayttajat: UserRepository,
    private val kayttajapalvelu: UserService
) {
    fun kasitteleKutsu(
        salain: Cipher, salausavain: SecretKey, hetu: String?, eppn: String?, etunimi: String, sukunimi: String
    ): User? {
        val pyyntoAttribuutit = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        pyyntoAttribuutit.request.getSession(false)?.removeAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE)
        val kutsutunniste = pyyntoAttribuutit.request.getParameter("RelayState")
        if (kutsutunniste == null || kutsutunniste == KIRJAUTUMISEN_OLETUS_RELAY_STATE) return null

        val kutsu = kutsut.findById(kutsutunniste).orElse(null) ?: return null
        val kutsuttuKayttaja = kayttajat.findByIdWithAuthorities(kutsu.user?.id.required()).get()
        try {
            kayttajapalvelu.createOrUpdateUserWithToken(kutsuttuKayttaja, kutsu, salain, salausavain, hetu, eppn, etunimi, sukunimi)
        } catch (ex: Exception) {
            if (ex.message == LoginException.VIRHEELLINEN_NIMI.name) {
                pyyntoAttribuutit.request.session.setAttribute(
                    LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE,
                    LoginNameMismatch(
                        invitationName = listOfNotNull(kutsuttuKayttaja.firstName, kutsuttuKayttaja.lastName).joinToString(" "),
                        authenticatedName = "$etunimi $sukunimi"
                    )
                )
            }
            throw ex
        }
        return kutsuttuKayttaja
    }
}
