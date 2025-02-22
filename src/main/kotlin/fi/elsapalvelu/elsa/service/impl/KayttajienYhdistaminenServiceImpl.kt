package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.KayttajienYhdistaminenService
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenResult
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KayttajienYhdistaminenServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val entityManager: EntityManager
) : KayttajienYhdistaminenService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun yhdistaKayttajatilit(kayttajienYhdistaminenDTo: KayttajienYhdistaminenDTO): List<KayttajienYhdistaminenResult> {
        val tilanne: ArrayList<KayttajienYhdistaminenResult> = arrayListOf()
        val ensimmainenKayttaja = kayttajaRepository.findById(kayttajienYhdistaminenDTo.ensimmainenKayttajaId!!)
        val toinenKayttaja = kayttajaRepository.findById(kayttajienYhdistaminenDTo.toinenKayttajaId!!)

        val ensimmainenAuthority: Authority? = ensimmainenKayttaja.get().user?.activeAuthority
        val toinenAuthority: Authority? = toinenKayttaja.get().user?.activeAuthority

        if (ensimmainenAuthority == Authority(ERIKOISTUVA_LAAKARI) || ensimmainenAuthority == Authority(YEK_KOULUTETTAVA)
            && toinenAuthority == Authority(KOULUTTAJA))
        {
            log.info(
                "Käyttäjätilien yhdistäminen kutsuttu erikoistuvalle id {} ja kouluttajalle id {}",
                ensimmainenKayttaja.get().id, toinenKayttaja.get().id
            )

            kasitteleKouluttajavaltuutukset(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleKayttajaYliopistoErikoisalat(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())

            kasitteleKoejaksonAloituskeskustelut(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleKoejaksonValiarvioinnit(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleKoejaksonLoppukeskustelut(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleKoejaksonKehittamistoimenpiteet(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())

            kasitteleSeurantajaksot(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleKoulutussopimuket(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            kasitteleSuoritusArvioinnnit(tilanne, ensimmainenKayttaja.get(), toinenKayttaja.get())
            poistaVerificationToken(tilanne, toinenKayttaja.get())
            poistaToinenKayttaja(tilanne, toinenKayttaja.get())
            lisaaKayttajalleRooli(tilanne, ensimmainenKayttaja.get().user!!, Authority(KOULUTTAJA))

            try {
                val ensimmainenUser = ensimmainenKayttaja.get().user
                ensimmainenUser!!.email = kayttajienYhdistaminenDTo.yhteinenSahkoposti
                userRepository.save(ensimmainenUser)
                log.info(
                    "YhteinenSahkoposti vaihdettu sähköpostiosoite käyttäjälle id:llä {}",
                    ensimmainenKayttaja.get().id
                )
                tilanne.add(KayttajienYhdistaminenResult("YhteinenSahkoposti", true))
            } catch (e: Exception) {
                tilanne.add(KayttajienYhdistaminenResult("YhteinenSahkoposti", false))
                log.error("YhteinenSahkoposti sähköpostin vaihto virhe {}", e.message.toString())
            }

            log.info(
                "Käyttäjätilien yhdistäminen suoritettu erikoistuvalle id {} ja kouluttajalle id {}",
                ensimmainenKayttaja.get().id, toinenKayttaja.get().id
            )
        } else {
            throw BadRequestAlertException(
                "Valituilla käyttäjillä on roolit joiden yhdistystä ei tällä hetkellä tueta",
                "user", "invalidauthorities"
            )
        }

        // forced rollback for debugging reasons
        // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()

        return tilanne
    }

    // perustuu oletukseen että ensimmäinen on erikoistuja (jää) ja toinen kouluttaja (poistetaan)
    private fun kasitteleKouluttajavaltuutukset(
        tilanne: ArrayList<KayttajienYhdistaminenResult>, ensimmainenKayttaja: Kayttaja, toinenKayttaja: Kayttaja
    ) {
        try {
            val valtuutukset = kouluttajavaltuutusRepository.findByValtuutettuId(toinenKayttaja.id!!)
            valtuutukset.forEach {
                val olemassaOlevaValtuutus = kouluttajavaltuutusRepository
                    .findByValtuuttajaOpintooikeusIdAndValtuutettuUserId(it.valtuuttajaOpintooikeus!!.id!!,
                        ensimmainenKayttaja.user!!.id!!
                    )
                if (olemassaOlevaValtuutus.isEmpty) {
                    it.valtuutettu = ensimmainenKayttaja
                    kouluttajavaltuutusRepository.save(it)
                    log.info(
                        "Siirretty kouluttajavaltuutus id {} käyttäjän nimiin id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                } else {
                    kouluttajavaltuutusRepository.delete(it)
                    log.info(
                        "Kouluttajavaltuutus id {} käyttäjälle id:llä {} oli jo olemassa, joten poistettiin siirrettävä rivi.",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
            }
            tilanne.add(KayttajienYhdistaminenResult("Kouluttajavaltuutukset", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("Kouluttajavaltuutukset", false))
            log.error("Kouluttajavaltuutus käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleKayttajaYliopistoErikoisalat(
        tilanne: ArrayList<KayttajienYhdistaminenResult>, ensimmainenKayttaja: Kayttaja, toinenKayttaja: Kayttaja
    ) {
        try {
            toinenKayttaja.yliopistotAndErikoisalat.forEach {
                val exists = kayttajaYliopistoErikoisalaRepository.findAllByYliopistoIdAndErikoisalaIdAndKayttajaId(
                    it.yliopisto!!.id!!, it.erikoisala!!.id!!, ensimmainenKayttaja.id!!
                )
                if (exists.isEmpty()) {
                    kayttajaYliopistoErikoisalaRepository.save(
                        KayttajaYliopistoErikoisala(
                            kayttaja = ensimmainenKayttaja,
                            erikoisala = it.erikoisala,
                            yliopisto = it.yliopisto
                        )
                    )
                }
                kayttajaYliopistoErikoisalaRepository.delete(it)
                log.info(
                    "Poistettu KayttajaYliopistoErikoisalat id {} ja luotu uusi vastaava käyttäjälle id:llä {}",
                    it.id,
                    ensimmainenKayttaja.id
                )
            }
            tilanne.add(KayttajienYhdistaminenResult("KayttajaYliopistoErikoisalat", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("KayttajaYliopistoErikoisalat", false))
            log.error("KayttajaYliopistoErikoisalat käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleKoejaksonAloituskeskustelut(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        ensimmainenKayttaja: Kayttaja,
        toinenKayttaja: Kayttaja
    ) {
        try {
            val aloituskeskustelut = koejaksonAloituskeskusteluRepository
                .findAllByLahikouluttajaUserIdOrLahiesimiesUserId(toinenKayttaja.user!!.id!!)
            aloituskeskustelut.forEach {
                if (it.lahikouluttaja!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahikouluttaja = ensimmainenKayttaja
                    log.info(
                        "KoejaksonAloituskeskustelut id {} lahikouluttaja id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                if (it.lahiesimies!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahiesimies = ensimmainenKayttaja
                    log.info(
                        "KoejaksonAloituskeskustelut id {} lahiesimies id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                koejaksonAloituskeskusteluRepository.save(it)
            }
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonAloituskeskustelut", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonAloituskeskustelut", false))
            log.error("KoejaksonAloituskeskustelut käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleKoejaksonValiarvioinnit(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        ensimmainenKayttaja: Kayttaja,
        toinenKayttaja: Kayttaja
    ) {
        try {
            val valiarvioinnit = koejaksonValiarviointiRepository
                .findAllByLahikouluttajaUserIdOrLahiesimiesUserId(toinenKayttaja.user!!.id!!)
            valiarvioinnit.forEach {
                if (it.lahikouluttaja!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahikouluttaja = ensimmainenKayttaja
                    log.info(
                        "KoejaksonValiarviointi id {} lahikouluttaja id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                if (it.lahiesimies!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahiesimies = ensimmainenKayttaja
                    log.info(
                        "KoejaksonValiarviointi id {} lahiesimies id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                koejaksonValiarviointiRepository.save(it)
            }
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonValiarviointi", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonValiarviointi", false))
            log.error("KoejaksonValiarviointi käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleKoejaksonLoppukeskustelut(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        ensimmainenKayttaja: Kayttaja,
        toinenKayttaja: Kayttaja
    ) {
        try {
            val loppukeskustelut = koejaksonLoppukeskusteluRepository
                .findAllByLahikouluttajaUserIdOrLahiesimiesUserId(toinenKayttaja.user!!.id!!)
            loppukeskustelut.forEach {
                if (it.lahikouluttaja!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahikouluttaja = ensimmainenKayttaja
                    log.info(
                        "KoejaksonLoppukeskustelu id {} lahikouluttaja id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                if (it.lahiesimies!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahiesimies = ensimmainenKayttaja
                    log.info(
                        "KoejaksonLoppukeskustelu id {} lahiesimies id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                koejaksonLoppukeskusteluRepository.save(it)
            }
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonLoppukeskustelu", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonLoppukeskustelu", false))
            log.error("KoejaksonLoppukeskustelu käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleKoejaksonKehittamistoimenpiteet(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        ensimmainenKayttaja: Kayttaja,
        toinenKayttaja: Kayttaja
    ) {
        try {
            val kehittamistoimenpiteet = koejaksonKehittamistoimenpiteetRepository
                .findAllByLahikouluttajaUserIdOrLahiesimiesUserId(toinenKayttaja.user!!.id!!)
            kehittamistoimenpiteet.forEach {
                if (it.lahikouluttaja!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahikouluttaja = ensimmainenKayttaja
                    log.info(
                        "KoejaksonKehittamistoimenpiteet id {} lahikouluttaja id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                if (it.lahiesimies!!.id!!.equals(toinenKayttaja.id!!)) {
                    it.lahiesimies = ensimmainenKayttaja
                    log.info(
                        "KoejaksonKehittamistoimenpiteet id {} lahiesimies id vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                koejaksonKehittamistoimenpiteetRepository.save(it)
            }
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonKehittamistoimenpiteet", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("KoejaksonKehittamistoimenpiteet", false))
            log.error("KoejaksonKehittamistoimenpiteet käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleKoulutussopimuket(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        ensimmainenKayttaja: Kayttaja,
        toinenKayttaja: Kayttaja
    ) {
        try {
            val koulutussopimukset = koejaksonKoulutussopimusRepository
                .findAllByKouluttajatKouluttajaUserId(toinenKayttaja.user!!.id!!)
            koulutussopimukset.forEach {
                it.kouluttajat!!.forEach { k ->
                    if (k.kouluttaja!!.id!!.equals(toinenKayttaja.id)) {
                        k.kouluttaja = ensimmainenKayttaja
                        log.info(
                            "Koulutussopimuket id {} kouluttajat rivi id {} kouluttaja id tieto vaihdettu käyttäjään id:llä {}",
                            it.id,
                            k.id,
                            ensimmainenKayttaja.id
                        )
                    }
                }
                koejaksonKoulutussopimusRepository.save(it)
            }
            tilanne.add(KayttajienYhdistaminenResult("Koulutussopimuket", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("Koulutussopimuket", false))
            log.error("Koulutussopimuket käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun kasitteleSuoritusArvioinnnit(
        tilanne: ArrayList<KayttajienYhdistaminenResult>, ensimmainenKayttaja: Kayttaja, toinenKayttaja: Kayttaja
    ) {
        try {
            val suoritusarvioinnit =
                suoritusarviointiRepository.findAllByArvioinninAntajaUserId(toinenKayttaja.user!!.id!!)
            suoritusarvioinnit.forEach {
                if (it.arvioinninAntaja!!.id!!.equals(toinenKayttaja.id)) {
                    it.arvioinninAntaja = ensimmainenKayttaja
                    log.info(
                        "SuoritusArvioinnnit id {} arvioinninAntaja id tieto vaihdettu käyttäjään id:llä {}",
                        it.id,
                        ensimmainenKayttaja.id
                    )
                }
                suoritusarviointiRepository.save(it)
            }
            tilanne.add(KayttajienYhdistaminenResult("SuoritusArvioinnnit", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("SuoritusArvioinnnit", false))
            log.error("SuoritusArvioinnnit käsittelyssä virhe {}", e.message.toString())
        }
    }

    private fun poistaVerificationToken(tilanne: ArrayList<KayttajienYhdistaminenResult>, toinenKayttaja: Kayttaja) {
        try {
            val token = verificationTokenRepository.findOneByUserId(toinenKayttaja.user!!.id!!)
            if (token != null) {
                verificationTokenRepository.delete(token)
                tilanne.add(KayttajienYhdistaminenResult("VerificationToken", true))
                log.info(
                    "VerificationToken id {} poistettu käyttäjälle id:llä {}",
                    token.id,
                    toinenKayttaja.id
                )
            } else {
                log.warn(
                    "VerificationToken ei löytynyt käyttäjälle id:llä {}",
                    toinenKayttaja.id
                )
            }
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("VerificationToken", false))
            log.error("VerificationToken poistossa virhe {}", e.message.toString())
        }
    }

    private fun poistaToinenKayttaja(tilanne: ArrayList<KayttajienYhdistaminenResult>, toinenKayttaja: Kayttaja) {
        try {
            kayttajaRepository.delete(toinenKayttaja)
            toinenKayttaja.user?.let { userRepository.delete(it) }
            entityManager.flush()

            log.info(
                "poistaToinenKayttaja poistettu käyttäjä id:llä {}",
                toinenKayttaja.id
            )
            tilanne.add(KayttajienYhdistaminenResult("poistaToinenKayttaja", true))
        } catch (e: Exception) {
            tilanne.add(KayttajienYhdistaminenResult("poistaToinenKayttaja", false))
            log.error("poistaToinenKayttaja poistossa virhe {}", e.message.toString())
        }
    }

    private fun lisaaKayttajalleRooli(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        user: User,
        authority: Authority
    ) {
        if (!user.authorities.contains(authority)) {
            user.authorities.add(authority)
            userRepository.save(user)
            log.info(
                "lisaaKayttajalleRooli lisätty rooli {} käyttäjälle jhi_user id:llä {}",
                authority.name,
                user.id
            )
            tilanne.add(KayttajienYhdistaminenResult("lisaaKayttajalleRooli", true))
        }
    }

    private fun kasitteleSeurantajaksot(
        tilanne: ArrayList<KayttajienYhdistaminenResult>,
        ensimmainenKayttaja: Kayttaja,
        toinenKayttaja: Kayttaja
    ) {
        try {
            val seurantajaksot = seurantajaksoRepository
                .findAllByKouluttajaUserId(toinenKayttaja.user!!.id!!)
            seurantajaksot.forEach {
                    if (it.kouluttaja!!.id!!.equals(toinenKayttaja.id)) {
                        it.kouluttaja = ensimmainenKayttaja
                        log.info(
                            "Seurantajakso id {} kouluttaja id tieto vaihdettu käyttäjään id:llä {}",
                            it.id,
                            ensimmainenKayttaja.id
                        )
                    }
                    seurantajaksoRepository.save(it)
                }
                tilanne.add(KayttajienYhdistaminenResult("Seurantajaksot", true))
            } catch (e: Exception) {
                tilanne.add(KayttajienYhdistaminenResult("Seurantajaksot", false))
                log.error("Seurantajaksot käsittelyssä virhe {}", e.message.toString())
            }
        }

}
