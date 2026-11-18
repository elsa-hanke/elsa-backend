package fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen

import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.security.HENKILOKUNNAN_ROOLIT
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KirjautumisenKayttooikeuspalvelu(
    private val kayttajat: KayttajaRepository,
    private val opintooikeudet: OpintooikeusRepository,
    private val opintooikeuspalvelu: OpintooikeusService,
    private val kayttajapalvelu: UserService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun tarkistaKayttooikeudet(kayttajatunnus: String, etunimi: String, sukunimi: String): Kirjautumistulos {
        val kayttaja = kayttajat.findOneByUserIdWithAuthorities(kayttajatunnus).orElseThrow {
            Exception(LoginException.EI_KAYTTO_OIKEUTTA.name)
        }

        if (kayttaja.tila == KayttajatilinTila.PASSIIVINEN) {
            log.error("Kirjautuminen epäonnistui käyttäjälle $etunimi $sukunimi. Käyttäjän tili on passivoitu")
            throw Exception(LoginException.TILI_PASSIVOITU.name)
        }

        val kayttajatili = kayttaja.user.required()
        if (kayttajatili.authorities.contains(Authority(name = VASTUUHENKILO)) && kayttaja.tila == KayttajatilinTila.KUTSUTTU) {
            kayttaja.tila = KayttajatilinTila.AKTIIVINEN
            kayttajat.save(kayttaja)
        }
        val kirjautuvanKayttajanRoolit = tarkistaKirjautuvanKayttajanRoolit(kayttajatili)
        if (kirjautuvanKayttajanRoolit.isEmpty()) {
            val virhe = if (opintooikeudet.findAllByErikoistuvaLaakariKayttajaUserId(kayttajatili.id.required()).isNotEmpty()) {
                LoginException.EI_OPINTO_OIKEUTTA
            } else {
                LoginException.EI_KAYTTO_OIKEUTTA
            }
            log.warn("Kirjautuminen epäonnistui käyttäjälle {}: {}", kayttajatili.id, virhe.name)
            throw Exception(virhe.name)
        }

        return Kirjautumistulos(kayttajatili, kirjautuvanKayttajanRoolit)
    }

    private fun tarkistaKirjautuvanKayttajanRoolit(kayttajatili: User): Set<Authority> {
        val henkilokunnanRoolit = kayttajatili.authorities.filter { it.name in HENKILOKUNNAN_ROOLIT }.toSet()
        val aktiivinenRooli = kayttajatili.activeAuthority
        return try {
            // Opinto-oikeudet tarkistetaan jokaisella kirjautumisella riippumatta käyttäjän nykyisistä rooleista.
            opintooikeuspalvelu.checkOpintooikeusAndRoles(kayttajatili)
            kayttajatili.authorities.toSet()
        } catch (ex: Exception) {
            if (henkilokunnanRoolit.isEmpty()) throw ex
            // Tarkistusvirheessä sallitaan vain henkilökunnan aiemmat roolit ilman User-olion yhdistämistä tietokantaan.
            log.warn("Opinto-oikeuksien tarkistus epäonnistui käyttäjälle {}. Kirjautuminen vain henkilökunnan rooleilla.", kayttajatili.id, ex)
            if (aktiivinenRooli !in henkilokunnanRoolit) {
                kayttajapalvelu.updateRooli(henkilokunnanRoolit.first().name.required(), kayttajatili.id.required())
            }
            henkilokunnanRoolit
        }
    }
}
