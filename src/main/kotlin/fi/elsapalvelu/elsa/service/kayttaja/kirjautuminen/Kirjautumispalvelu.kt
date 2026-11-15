package fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import jakarta.persistence.EntityNotFoundException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT

@Service
class Kirjautumispalvelu(
    private val kayttajapalvelu: UserService,
    private val kayttajat: UserRepository,
    private val kutsupalvelu: Kirjautumiskutsupalvelu,
    private val opintotietopalvelu: KirjautumisenOpintotietopalvelu,
    private val kayttooikeuspalvelu: KirjautumisenKayttooikeuspalvelu,
    private val asetukset: ApplicationProperties,
    private val ymparisto: Environment
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun kirjaudu(hetu: String?, eppn: String?, etunimi: String, sukunimi: String): Kirjautumistulos {
        val avainTavut = Base64.getDecoder().decode(asetukset.getSecurity().encodedKey)
        val salausavain: SecretKey = SecretKeySpec(avainTavut, 0, avainTavut.size, asetukset.getSecurity().secretKeyAlgorithm)

        val salain = Cipher.getInstance(asetukset.getSecurity().cipherAlgorithm)
        val kayttaja = haeKirjautuvaKayttaja(salain, salausavain, hetu, eppn, etunimi, sukunimi)
        return kayttooikeuspalvelu.tarkistaKayttooikeudet(kayttaja.id.required(), etunimi, sukunimi)
    }

    internal fun haeKirjautuvaKayttaja(
        salain: Cipher, salausavain: SecretKey, hetu: String?, eppn: String?, etunimi: String, sukunimi: String
    ): User {
        val kutsuttuKayttaja = kutsupalvelu.kasitteleKutsu(salain, salausavain, hetu, eppn, etunimi, sukunimi)

        var olemassaOlevaKayttaja = kayttajapalvelu.findExistingUser(salain, salausavain, hetu, eppn)

        if (haetaankoOpintotiedot(olemassaOlevaKayttaja, kutsuttuKayttaja, hetu)) {
            requireNotNull(hetu)

            if (olemassaOlevaKayttaja == null) {
                opintotietopalvelu.luoEnsikirjautumisenOpintotiedot(salain, salausavain, hetu, etunimi, sukunimi)
                olemassaOlevaKayttaja = kayttajapalvelu.findExistingUser(salain, salausavain, hetu, null)

                // Lokaalissa ympäristössä luodaan uusi käyttäjä, jos sitä ei löydy.
                if (olemassaOlevaKayttaja == null && ymparisto.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT)) {
                    opintotietopalvelu.luoKayttajaKehitysymparistossa(salain, salausavain, hetu, etunimi, sukunimi)
                    olemassaOlevaKayttaja = kayttajapalvelu.findExistingUser(salain, salausavain, hetu, null)
                }

                olemassaOlevaKayttaja?.let { kayttaja -> opintotietopalvelu.paivitaOpintosuorituksetTaustalla(kayttaja.id.required(), hetu) }
            } else {
                opintotietopalvelu.paivitaOpintotiedot(olemassaOlevaKayttaja.id.required(), hetu, etunimi, sukunimi)
                opintotietopalvelu.paivitaOpintosuorituksetTaustalla(olemassaOlevaKayttaja.id.required(), hetu)

                olemassaOlevaKayttaja = kayttajat.findByIdWithAuthorities(olemassaOlevaKayttaja.id.required()).orElseThrow { EntityNotFoundException("Käyttäjää ei löydy") }
            }
        }

        if (olemassaOlevaKayttaja == null) {
            log.warn("Kirjautuminen epäonnistui käyttäjälle $etunimi $sukunimi (eppn $eppn). Käyttäjällä ei ole käyttöoikeutta.")
            throw Exception(LoginException.EI_KAYTTO_OIKEUTTA.name)
        } else {
            // Tarkistetaan, että käyttäjän nimi on ajan tasalla
            if ((etunimi != "" && sukunimi != "") && (olemassaOlevaKayttaja.firstName != etunimi || olemassaOlevaKayttaja.lastName != sukunimi)) {
                olemassaOlevaKayttaja.firstName = etunimi
                olemassaOlevaKayttaja.lastName = sukunimi
                kayttajat.save(olemassaOlevaKayttaja)
            }
        }
        return olemassaOlevaKayttaja
    }

    private fun haetaankoOpintotiedot(
        olemassaOlevaKayttaja: User?,
        kutsuttuKayttaja: User?,
        hetu: String?
    ): Boolean =
        asetukset.getStudyDataIntegrations().enabled &&
            (olemassaOlevaKayttaja == null || !onkoRooleja(olemassaOlevaKayttaja) ||
            onkoErikoistuvaLaakari(olemassaOlevaKayttaja) || onkoYekKoulutettava(olemassaOlevaKayttaja) ||
            onkoKouluttaja(olemassaOlevaKayttaja)
            )
            && (kutsuttuKayttaja == null || !onkoRooleja(kutsuttuKayttaja) ||
            onkoErikoistuvaLaakari(kutsuttuKayttaja) || onkoYekKoulutettava(kutsuttuKayttaja)
                ) && hetu != null

    private fun onkoErikoistuvaLaakari(kayttaja: User): Boolean =
        kayttaja.authorities.contains(Authority(name = ERIKOISTUVA_LAAKARI))

    private fun onkoYekKoulutettava(kayttaja: User): Boolean =
        kayttaja.authorities.contains(Authority(name = YEK_KOULUTETTAVA))

    private fun onkoKouluttaja(kayttaja: User): Boolean =
        kayttaja.authorities.contains(Authority(name = KOULUTTAJA))

    private fun onkoRooleja(kayttaja: User): Boolean =
        kayttaja.authorities.isNotEmpty()
}
