package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.KayttajienYhdistaminenService
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KayttajienYhdistaminenServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val userRepository: UserRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val userService: UserService,
    private val kouluttajavaltuutusService: KouluttajavaltuutusService,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val opintooikeusService: OpintooikeusService,
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val verificationTokenRepository: VerificationTokenRepository
) : KayttajienYhdistaminenService {

    @Transactional
    override fun yhdistaKayttajatilit(kayttajienYhdistaminenDTo: KayttajienYhdistaminenDTO) {
        val ensimmainenKayttaja = kayttajaRepository.findById(kayttajienYhdistaminenDTo.ensimmainenKayttajaId!!)
        val toinenKayttaja = kayttajaRepository.findById(kayttajienYhdistaminenDTo.toinenKayttajaId!!)

        val ensimmainenAuthority: Authority? = ensimmainenKayttaja.get().user?.activeAuthority
        val toinenAuthority: Authority? = toinenKayttaja.get().user?.activeAuthority

        if (ensimmainenAuthority == Authority(ERIKOISTUVA_LAAKARI) && toinenAuthority == Authority(KOULUTTAJA)) {

            kasitteleKouluttajavaltuutukset(ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleKayttajaYliopistoErikoisalat(ensimmainenKayttaja.get(), toinenKayttaja.get())
            poistaVerificationToken(toinenKayttaja.get())
            poistaToinenKayttaja(toinenKayttaja.get())
            lisaaKayttajalleRooli(ensimmainenKayttaja.get().user!!, Authority(KOULUTTAJA))


            val ensimmainenUser = ensimmainenKayttaja.get().user
            ensimmainenUser!!.email = kayttajienYhdistaminenDTo.yhteinenSahkoposti
            userRepository.save(ensimmainenUser)
        } else {
            throw BadRequestAlertException(
                "Valituilla käyttäjillä on roolit joiden yhdistystä ei tällä hetkellä tueta",
                "user", "invalidauthorities"
            )
        }
    }

    // perustuu oletukseen että ensimmäinen on erikoistuja (jää) ja toinen kouluttaja (poistetaan)
    private fun kasitteleKouluttajavaltuutukset(ensimmainenKayttaja: Kayttaja, toinenKayttaja: Kayttaja) {
        val valtuutukset = kouluttajavaltuutusRepository.findByValtuutettuId(toinenKayttaja.id!!)
        valtuutukset.forEach {
            it.valtuutettu = ensimmainenKayttaja
            kouluttajavaltuutusRepository.save(it)
        }
    }

    private fun kasitteleKayttajaYliopistoErikoisalat(ensimmainenKayttaja: Kayttaja, toinenKayttaja: Kayttaja) {
        toinenKayttaja.yliopistotAndErikoisalat.forEach {
            val exists = ensimmainenKayttaja.yliopistotAndErikoisalat.any { ek ->
                ek.yliopisto == it.yliopisto && ek.erikoisala == it.erikoisala
            }
            if (!exists) {
                kayttajaYliopistoErikoisalaRepository.save(
                    KayttajaYliopistoErikoisala(
                        kayttaja = ensimmainenKayttaja,
                        erikoisala = it.erikoisala,
                        yliopisto = it.yliopisto
                    )
                )
            }
            kayttajaYliopistoErikoisalaRepository.delete(it)
        }
    }

    private fun poistaVerificationToken(toinenKayttaja: Kayttaja) {
        val token = verificationTokenRepository.findOneByUserId(toinenKayttaja.user!!.id!!)
        token?.let { verificationTokenRepository.delete(it) }
    }

    private fun poistaToinenKayttaja(toinenKayttaja: Kayttaja) {
        kayttajaRepository.delete(toinenKayttaja)
    }

    private fun lisaaKayttajalleRooli(user: User, authority: Authority) {
        if (!user.authorities.contains(authority)) {
            user.authorities.add(authority)
            userRepository.save(user)
        }
    }


    // val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(ensimmainenKayttaja.id!!)
}
