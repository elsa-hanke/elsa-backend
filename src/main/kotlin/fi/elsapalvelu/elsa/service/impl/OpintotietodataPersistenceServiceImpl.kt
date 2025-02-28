package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.periodLessThan
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.OpintotietodataPersistenceService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.crypto.Cipher
import javax.crypto.SecretKey
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional

private const val SUU_JA_LEUKAKIRURGIA_VIRTA_PATEVYYSKOODI = "esl"
private const val PAATTYNEEN_OPINTOOIKEUDEN_KATSELUAIKA_KUUKAUDET = 6L

@Service
@Transactional
class OpintotietodataPersistenceServiceImpl(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val yliopistoRepository: YliopistoRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val opintoopasRepository: OpintoopasRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val asetusRepository: AsetusRepository,
    private val erikoisalaSisuTutkintoohjelmaRepository: ErikoisalaSisuTutkintoohjelmaRepository,
    private val clock: Clock,
    private val opintooikeusHerateRepository: OpintooikeusHerateRepository,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties
) : OpintotietodataPersistenceService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        etunimi: String,
        sukunimi: String,
        opintotietodataDTOs: List<OpintotietodataDTO>
    ) {
        val filteredOpintotietodataOpintooikeudet =
            filterOpintooikeudetByVoimassaDate(opintotietodataDTOs.map {
                it.opintooikeudet ?: listOf()
            }.flatten())

        if (filteredOpintotietodataOpintooikeudet.isEmpty()) {
            // Etsitään, jos jokin opinto-oikeus on alkamassa tulevaisuudessa
            opintotietodataDTOs.map { dto ->
                dto.opintooikeudet?.map {
                    if (it.opintooikeudenAlkamispaiva?.isAfter(LocalDate.now(clock)) != false) throw Exception(LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name)
                }
            }
            log.info("Voimassaolevia opinto-oikeuksia ei löytynyt käyttäjälle $etunimi $sukunimi")
            return
        }

        val syntymaaika = checkSyntymaaikaValidDateExistsOrLogError(
            opintotietodataDTOs,
            etunimi,
            sukunimi
        ) ?: return
        val erikoistuvaLaakari =
            createErikoistuvaLaakari(cipher, originalKey, hetu, etunimi, sukunimi, syntymaaika)
        val userId = erikoistuvaLaakari.kayttaja?.user?.id!!

        checkOpintooikeudetAmount(filteredOpintotietodataOpintooikeudet, erikoistuvaLaakari)

        filteredOpintotietodataOpintooikeudet.sortedBy { it.opintooikeudenPaattymispaiva }.forEach {
            createOpintooikeus(it, userId, erikoistuvaLaakari)
        }
    }

    override fun createOrUpdateIfChanged(
        userId: String,
        etunimi: String,
        sukunimi: String,
        opintotietodataDTOs: List<OpintotietodataDTO>
    ) {
        val syntymaaika = checkSyntymaaikaValidDateExistsOrLogError(
            opintotietodataDTOs,
            etunimi,
            sukunimi
        ) ?: return
        try {
            val authority =
                userRepository.findByIdWithAuthorities(userId).map { return@map it.authorities.first() }.orElse(Authority("ROLE_USER"))
            userRepository.setActiveAuthorityIfNull(userId, authority)
        } catch (e: Exception) {
            log.warn("Käyttäjällä ei välttämättä ole aktiivista authoriteettia, mikä voi vaikuttaa tietojen hakuun.")
        }
        var erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        val opintotietodataOpintooikeudet =
            opintotietodataDTOs.map { it.opintooikeudet ?: listOf() }.flatten()

        if (filterOpintooikeudetByVoimassaDate(opintotietodataOpintooikeudet).isEmpty()) {
            val erikoistuvaAuthorities = arrayOf(Authority("ROLE_ERIKOISTUVA_LAAKARI"), Authority("ROLE_YEK_KOULUTETTAVA"))
            if (opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
                    userId,
                    LocalDate.now(clock),
                    OpintooikeudenTila.allowedTilat(),
                    OpintooikeudenTila.endedTilat()).isEmpty()
                && (userRepository.findByIdWithAuthorities(userId).map { return@map it.authorities.toList().all { auth -> auth in erikoistuvaAuthorities }}).orElse(true)
            ) {
                // Etsitään, jos jokin opinto-oikeus on alkamassa tulevaisuudessa
                opintotietodataOpintooikeudet.map {
                    if (it.opintooikeudenAlkamispaiva?.isAfter(LocalDate.now(clock)) != false) throw Exception(
                        LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name
                    )
                }
            }
            return
        }

        if (erikoistuvaLaakari == null) {
            erikoistuvaLaakari = erikoistuvaLaakariRepository.save(
                ErikoistuvaLaakari(
                    syntymaaika = syntymaaika,
                    kayttaja = kayttajaRepository.findOneByUserId(userId)
                        .orElseThrow { EntityNotFoundException("Käyttäjää ei löydy.") })
            )
        }

        erikoistuvaLaakari?.let {
            updateNimiIfChanged(it, etunimi, sukunimi)

            checkOpintooikeudetAmount(
                filterOpintooikeudetByVoimassaDate(opintotietodataOpintooikeudet),
                it
            )
            updateOpintooikeudet(userId, opintotietodataOpintooikeudet, it)
        }
    }

    override fun createOrUpdateOpintotieto(userId: String, opintotietodataDTO: OpintotietodataDTO) {
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
            ?: throw EntityNotFoundException("Erikoistuvaa lääkäriä ei löydy.")
        opintotietodataDTO.opintooikeudet?.let {
            updateOpintooikeudet(
                userId,
                it,
                erikoistuvaLaakari
            )
        }
    }

    override fun createWithoutOpintotietodata(
        cipher: Cipher, originalKey: SecretKey, hetu: String?, etunimi: String, sukunimi: String
    ) {
        val erikoistuvaLaakari = createErikoistuvaLaakari(
            cipher,
            originalKey,
            hetu,
            etunimi,
            sukunimi,
            LocalDate.now(ZoneId.systemDefault()).minusYears(40)
        )
        val yliopisto = yliopistoRepository.findByIdOrNull(1)
        val erikoisala = erikoisalaRepository.findByIdOrNull(46)
        val opintoopas = opintoopasRepository.findByIdOrNull(15)
        val asetus = asetusRepository.findByIdOrNull(5)

        var opintooikeus = Opintooikeus(
            opintooikeudenMyontamispaiva = LocalDate.now(ZoneId.systemDefault()),
            opintooikeudenPaattymispaiva = LocalDate.now(ZoneId.systemDefault()).plusYears(10),
            opiskelijatunnus = "123456",
            asetus = asetus,
            osaamisenArvioinninOppaanPvm = LocalDate.now(ZoneId.systemDefault()),
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala,
            opintoopas = opintoopas,
            kaytossa = true,
            tila = OpintooikeudenTila.AKTIIVINEN
        )
        opintooikeus = opintooikeusRepository.save(opintooikeus)

        erikoistuvaLaakari.kayttaja?.user?.let {
            it.authorities.add(Authority(name = ERIKOISTUVA_LAAKARI))
            it.activeAuthority = Authority(name = ERIKOISTUVA_LAAKARI)
            userRepository.save(it)
        }
        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakari.aktiivinenOpintooikeus = opintooikeus.id
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
    }

    private fun filterOpintooikeudetByVoimassaDate(opintooikeudet: List<OpintotietoOpintooikeusDataDTO>):
        List<OpintotietoOpintooikeusDataDTO> =
        opintooikeudet.filter {
            (it.opintooikeudenPaattymispaiva == null
            || !it.opintooikeudenPaattymispaiva!!.isBefore(LocalDate.now(clock)))
            && (it.opintooikeudenAlkamispaiva == null
            || !it.opintooikeudenAlkamispaiva!!.isAfter(LocalDate.now(clock)))
        }

    private fun checkOpintooikeudetAmount(
        opintooikeudet: List<OpintotietoOpintooikeusDataDTO>,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val yliopistot = opintooikeudet.map { it.yliopisto }.toSet()
        val user = erikoistuvaLaakari.kayttaja?.user!!
        var opintooikeusHerate: OpintooikeusHerate? = null
        opintooikeudet.takeIf { it.size > 2 }?.takeIf {
            // YEK opinto-oikeutta ei oteta huomioon opinto-oikeuksien määrän tarkistuksessa
            if (it.size == 3 && it.any { oikeus ->
                    findErikoisalaOrLogError(
                            oikeus.erikoisalaTunnisteList,
                            oikeus.yliopisto,
                            user.id!!
                        )?.id == YEK_ERIKOISALA_ID
            }) {
                false
            } else {
                opintooikeusHerate =
                    opintooikeusHerateRepository.findOneByErikoistuvaLaakariKayttajaUserId(user.id!!)
                opintooikeusHerate?.useaVoimassaolevaHerateLahetetty == null
            }
        }?.let {
            mailService.sendEmailFromTemplate(
                user,
                // Ei lähetetä Helsingin yliopiston virkailijoille: https://jira.eduuni.fi/browse/UOELSA-1156
                getOpintohallintoEmailAddresses(yliopistot.filter { it != YliopistoEnum.HELSINGIN_YLIOPISTO }
                    .toSet()),
                "useaOpintooikeus.html",
                "useaopintooikeus.title",
                properties = mapOf(
                    Pair(MailProperty.NAME, user.firstName + " " + user.lastName)
                )
            )

            opintooikeusHerate ?: OpintooikeusHerate(
                erikoistuvaLaakari = erikoistuvaLaakari
            ).apply {
                useaVoimassaolevaHerateLahetetty = Instant.now()
            }.let { opintooikeusHerateRepository.save(it) }
        }
    }

    private fun updateOpintooikeudet(
        userId: String,
        opintotietodataOpintooikeudet: List<OpintotietoOpintooikeusDataDTO>,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val existingOpintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        val opintotietodataNewOpintooikeudet =
            opintotietodataOpintooikeudet.filter { o -> o.id !in existingOpintooikeudet.map { it.yliopistoOpintooikeusId } }
        val opintotietodataExistingOpintooikeudet =
            opintotietodataOpintooikeudet.filter { o -> o.id in existingOpintooikeudet.map { it.yliopistoOpintooikeusId } }

        // Suodata uusista opinto-oikeuksista pois ne joiden päättymispäivä on menneisyydessä. Olemassaolevia ei
        // suodateta, koska päättymispäivä täytyy päivittää vaikka opinto-oikeus olisikin vanhentunut.
        val filteredOpintotietodataNewOpintooikeudet =
            filterOpintooikeudetByVoimassaDate(opintotietodataNewOpintooikeudet)
        val filteredOpintotietodataOpintooikeudet =
            filteredOpintotietodataNewOpintooikeudet + opintotietodataExistingOpintooikeudet

        filteredOpintotietodataOpintooikeudet.sortedBy { it.opintooikeudenPaattymispaiva }.forEach {
            createOrUpdateOpintooikeus(it, userId, erikoistuvaLaakari)
        }
    }

    private fun getOpintohallintoEmailAddresses(yliopistot: Set<YliopistoEnum>): List<String> {
        return yliopistot.mapNotNull { it.getOpintohallintoEmailAddress(applicationProperties) }
    }

    private fun createErikoistuvaLaakari(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        etunimi: String,
        sukunimi: String,
        syntymaaika: LocalDate
    ): ErikoistuvaLaakari {
        val userDTO = userService.createUser(cipher, originalKey, hetu, etunimi, sukunimi)
        val user = userRepository.findById(userDTO.id!!).orElseThrow()

        val kayttaja = kayttajaRepository.save(
            Kayttaja(
                user = user,
                tila = KayttajatilinTila.AKTIIVINEN
            )
        )

        var erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = kayttaja, syntymaaika = syntymaaika)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        return erikoistuvaLaakari
    }

    private fun createOpintooikeus(
        opintooikeusDTO: OpintotietoOpintooikeusDataDTO,
        userId: String,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val opintooikeusId =
            checkOpintooikeusIdValueExistsOrLogError(
                opintooikeusDTO.id,
                opintooikeusDTO.yliopisto,
                userId
            ) ?: return
        val yliopisto = findYliopistoOrLogError(opintooikeusDTO.yliopisto) ?: return
        val asetusStr =
            checkAsetusValueExistsOrLogError(
                opintooikeusDTO.asetus,
                opintooikeusDTO.yliopisto,
                userId
            ) ?: return
        val asetus = findAsetusOrLogError(asetusStr, opintooikeusDTO.yliopisto, userId) ?: return
        val opintooikeudenTila =
            checkOpintooikeudenTilaValueExistsOrLogError(
                opintooikeusDTO.tila,
                opintooikeusDTO.yliopisto,
                userId
            )
                ?: return
        var viimeinenKatselupaiva: LocalDate? = null
        if (OpintooikeudenTila.endedTilat().contains(opintooikeudenTila)) {
            viimeinenKatselupaiva = LocalDate.now(clock).plusMonths(
                PAATTYNEEN_OPINTOOIKEUDEN_KATSELUAIKA_KUUKAUDET)
        }
        val opintooikeudenAlkamispaiva = checkOpintooikeudenAlkamispaivaValidDateExistsOrLogError(
            opintooikeusDTO.opintooikeudenAlkamispaiva, opintooikeusDTO.yliopisto, userId
        ) ?: return
        val opintooikeudenPaattymispaiva =
            checkOpintooikeudenPaattymispaivaValidDateExistsOrLogError(
                opintooikeusDTO.opintooikeudenPaattymispaiva, opintooikeusDTO.yliopisto, userId
            ) ?: return
        val erikoisala =
            findErikoisalaOrLogError(
                opintooikeusDTO.erikoisalaTunnisteList,
                opintooikeusDTO.yliopisto,
                userId
            ) ?: return
        val opintoopas =
            findOpintoopasByErikoisalaAndVoimassaDateOrLogWarn(
                erikoisala.id!!,
                opintooikeudenAlkamispaiva,
                opintooikeusDTO.yliopisto,
                userId
            ) ?: findLatestOpintoopasByErikoisalaOrLogError(erikoisala.id!!) ?: return

        // Asetetaan mahdollisesti muille olemassaoleville opinto-oikeuksille kaytossa = false, koska käytössä voi
        // olla vain yksi kerrallaan.
        erikoistuvaLaakari.opintooikeudet.forEach {
            it.kaytossa = false
        }

        var opintooikeus = Opintooikeus(
            yliopistoOpintooikeusId = opintooikeusId,
            opintooikeudenMyontamispaiva = opintooikeudenAlkamispaiva,
            opintooikeudenPaattymispaiva = opintooikeudenPaattymispaiva,
            viimeinenKatselupaiva = viimeinenKatselupaiva,
            opiskelijatunnus = opintooikeusDTO.opiskelijatunnus,
            asetus = asetus,
            osaamisenArvioinninOppaanPvm = LocalDate.now(clock),
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala,
            opintoopas = opintoopas,
            kaytossa = true,
            tila = opintooikeudenTila,
            muokkausaika = Instant.now()
        )
        opintooikeus = opintooikeusRepository.save(opintooikeus)

        val user = erikoistuvaLaakari.kayttaja?.user!!
        val yek = opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID
        if (yek) {
            if (user.authorities.map { it.name }.contains(YEK_KOULUTETTAVA) == false) {
                user.authorities.add(Authority(name = YEK_KOULUTETTAVA))
            }

            user.activeAuthority = Authority(name = YEK_KOULUTETTAVA)
            userRepository.save(user)
        } else {
            if (user.authorities.map { it.name }.contains(ERIKOISTUVA_LAAKARI) == false) {
                user.authorities.add(Authority(name = ERIKOISTUVA_LAAKARI))
            }

            user.activeAuthority = Authority(name = ERIKOISTUVA_LAAKARI)
            userRepository.save(user)
            erikoistuvaLaakari.aktiivinenOpintooikeus = opintooikeus.id
        }
        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
    }

    private fun createOrUpdateOpintooikeus(
        opintooikeusDTO: OpintotietoOpintooikeusDataDTO,
        userId: String,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val existingOpintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        val opintooikeusId =
            checkOpintooikeusIdValueExistsOrLogError(
                opintooikeusDTO.id,
                opintooikeusDTO.yliopisto,
                userId
            )
                ?: return
        val yliopisto = findYliopistoOrLogError(opintooikeusDTO.yliopisto) ?: return
        val erikoisala = findErikoisalaOrLogError(
            opintooikeusDTO.erikoisalaTunnisteList,
            opintooikeusDTO.yliopisto,
            userId
        ) ?: return

        findExistingOpintooikeus(
            opintooikeusId,
            existingOpintooikeudet,
            erikoistuvaLaakari.id!!,
            yliopisto.id!!,
            erikoisala.id!!
        )?.also { opintooikeus ->
            val opintooikeudenTila = checkOpintooikeudenTilaValueExistsOrLogError(
                opintooikeusDTO.tila, opintooikeusDTO.yliopisto, userId
            ) ?: return
            val asetusStr =
                checkAsetusValueExistsOrLogError(
                    opintooikeusDTO.asetus,
                    opintooikeusDTO.yliopisto,
                    userId
                )
                    ?: return

            opintooikeusId.takeIf { opintooikeus.yliopistoOpintooikeusId == null }
                ?.let { opintooikeus.yliopistoOpintooikeusId = it }

            opintooikeusDTO.opintooikeudenPaattymispaiva?.takeIf { it != opintooikeus.opintooikeudenPaattymispaiva }
                ?.let {
                    opintooikeus.opintooikeudenPaattymispaiva = it
                }

            opintooikeudenTila.takeIf { it != opintooikeus.tila }?.let {
                if (OpintooikeudenTila.endedTilat().contains(opintooikeudenTila)) {
                    opintooikeus.viimeinenKatselupaiva = LocalDate.now(clock).plusMonths(
                        PAATTYNEEN_OPINTOOIKEUDEN_KATSELUAIKA_KUUKAUDET)
                }
                opintooikeus.tila = it
            }

            asetusStr.takeIf { it != opintooikeus.asetus?.nimi }?.let {
                findAsetusOrLogError(it, opintooikeusDTO.yliopisto, userId)?.let { asetus ->
                    opintooikeus.asetus = asetus
                    // Oppaan ja osaamisen arvioinnin pvm päivitys ei toimi oikein, poistetaan toistaiseksi
                    /*handleAsetusUpdated(
                        opintooikeus,
                        opintooikeusDTO.opintooikeudenPaattymispaiva,
                        opintooikeusDTO.yliopisto,
                        userId
                    )*/
                } ?: return
            }
            opintooikeus.muokkausaika = Instant.now()
        } ?: createOpintooikeus(
            opintooikeusDTO,
            userId,
            erikoistuvaLaakari
        )
    }

    private fun findExistingOpintooikeus(
        opintooikeusId: String,
        existingOpintooikeudet: List<Opintooikeus>,
        erikoistuvaLaakariId: Long,
        yliopistoId: Long,
        erikoisalaId: Long
    ): Opintooikeus? {
        // Päivitetään olemassaolevaa opinto-oikeutta jos sellainen löytyy Sisun/Pepin opinto-oikeuden id:llä
        // samasta yliopistosta (haettu ja tallennettu aiemmin) tai erikoisalan ja yliopiston id:llä
        // (erikoistuva kutsuttu aiemmin teknisen pääkäyttäjän toimesta).
        return existingOpintooikeudet.find { it.yliopistoOpintooikeusId == opintooikeusId && it.yliopisto?.id == yliopistoId }
            ?: opintooikeusRepository.findOneByErikoistuvaLaakariIdAndYliopistoIdAndErikoisalaIdAndYliopistoOpintooikeusIdIsNull(
                erikoistuvaLaakariId,
                yliopistoId,
                erikoisalaId
            )
    }

    private fun updateNimiIfChanged(
        erikoistuvaLaakari: ErikoistuvaLaakari, etunimi: String, sukunimi: String
    ) {
        val user = erikoistuvaLaakari.kayttaja?.user
        if (user?.firstName != etunimi) {
            user?.firstName = etunimi
        }
        if (user?.lastName != sukunimi) {
            user?.lastName = sukunimi
        }
    }

    private fun handleAsetusUpdated(
        existingOpintooikeus: Opintooikeus,
        newOpintooikeudenPaattymispaiva: LocalDate?,
        yliopisto: YliopistoEnum,
        userId: String
    ) {
        val opintooikeudenAlkamispaiva = existingOpintooikeus.opintooikeudenMyontamispaiva
        val erikoisala = existingOpintooikeus.erikoisala

        // Opinto-oikeuden pituus 10 vuotta lääketieteellä sekä hammaslääketieteen suu- ja leukakirurgian erikoisalalla.
        // Muutoin hammaslääketieteellä 6 vuotta.
        val defaultOpintooikeudenPituus =
            when (erikoisala?.tyyppi) {
                ErikoisalaTyyppi.LAAKETIEDE -> 10
                ErikoisalaTyyppi.HAMMASLAAKETIEDE -> {
                    if (erikoisala.virtaPatevyyskoodi == SUU_JA_LEUKAKIRURGIA_VIRTA_PATEVYYSKOODI) 10
                    else 6
                }

                else -> 10
            }
        // Jos opinto-oikeuden päättymispäivää ei ole asetettu tai opinto-oikeuden pituus on alle 10/6 vuotta
        // (määräaikainen opinto-oikeus), otetaan asetusmuutoksen yhteydessä käyttöön uusin opinto-opas.
        val useLatestOpintoopas =
            newOpintooikeudenPaattymispaiva == null || opintooikeudenAlkamispaiva!!.periodLessThan(
                newOpintooikeudenPaattymispaiva, defaultOpintooikeudenPituus
            )
        val opintoopas =
            if (useLatestOpintoopas) {
                findLatestOpintoopasByErikoisalaOrLogError(existingOpintooikeus.erikoisala?.id!!)
                    ?: return
            } else {
                val opintoopasValidDate =
                    newOpintooikeudenPaattymispaiva!!.minusYears(defaultOpintooikeudenPituus.toLong())
                findOpintoopasByErikoisalaAndVoimassaDateOrLogWarn(
                    existingOpintooikeus.erikoisala?.id!!,
                    opintoopasValidDate,
                    yliopisto,
                    userId
                )
                    ?: findLatestOpintoopasByErikoisalaOrLogError(existingOpintooikeus.erikoisala?.id!!)
                    ?: return
            }

        existingOpintooikeus.opintoopas = opintoopas
        existingOpintooikeus.osaamisenArvioinninOppaanPvm = opintoopas.voimassaoloAlkaa
    }

    private fun findYliopistoOrLogError(yliopisto: YliopistoEnum): Yliopisto? {
        return yliopistoRepository.findOneByNimi(yliopisto) ?: run {
            log.error("Yliopistoa $yliopisto ei löydy ELSA:n tietokannasta")
            return null
        }
    }

    private fun checkOpintooikeusIdValueExistsOrLogError(
        id: String?, yliopisto: YliopistoEnum, userId: String
    ): String? = id ?: run {
        log.warn("$yliopisto, user id: $userId. Opinto-oikeus id:tä ei ole asetettu.")
        return null
    }

    private fun checkOpintooikeudenAlkamispaivaValidDateExistsOrLogError(
        alkamispaiva: LocalDate?, yliopisto: YliopistoEnum, userId: String
    ): LocalDate? = alkamispaiva?.let {
        if (LocalDate.now(clock) >= alkamispaiva) {
            return alkamispaiva
        } else {
            log.warn("$yliopisto, user id: $userId. Opinto-oikeus ei ole vielä alkanut.")
            return null
        }
    } ?: run {
        log.error("$yliopisto, user id: $userId. Opinto-oikeuden alkamispäivää ei ole asetettu.")
        return null
    }

    private fun checkOpintooikeudenPaattymispaivaValidDateExistsOrLogError(
        paattymispaiva: LocalDate?, yliopisto: YliopistoEnum, userId: String
    ): LocalDate? = paattymispaiva ?: run {
        log.error("$yliopisto, user id: $userId. Opinto-oikeuden päättymispäivää ei ole asetettu.")
        return null
    }

    private fun checkAsetusValueExistsOrLogError(
        asetus: String?,
        yliopisto: YliopistoEnum,
        userId: String
    ): String? = asetus ?: run {
        log.error("$yliopisto, user id: $userId. Asetustietoa ei ole asetettu.")
        return null
    }

    private fun findAsetusOrLogError(
        asetus: String, yliopisto: YliopistoEnum, userId: String
    ): Asetus? {
        return asetus.let {
            asetusRepository.findOneByNimi(it) ?: run {
                log.error("$yliopisto, user id: $userId. Asetusta $it ei löydy ELSA:n tietokannasta.")
                return null
            }
        }
    }

    private fun findErikoisalaOrLogError(
        erikoisalaTunnisteList: List<String>?, yliopisto: YliopistoEnum, userId: String
    ): Erikoisala? {
        if (erikoisalaTunnisteList.isNullOrEmpty()) {
            log.error(
                "$yliopisto, user id: $userId. Erikoisalatietoa ei saatu opintotietojärjestelmästä."
            )
            return null
        }

        if (yliopisto == YliopistoEnum.HELSINGIN_YLIOPISTO) {
            return erikoisalaSisuTutkintoohjelmaRepository.findOneByTutkintoohjelmaId(
                erikoisalaTunnisteList.first()
            )?.erikoisala ?: run {
                log.error(
                    "$yliopisto, user id: $userId. Erikoisalaa ei löydy Elsan tietokannasta tutkinto-ohjelma id:n " +
                        "${erikoisalaTunnisteList.first()} perusteella."
                )
                return null
            }
        } else {
            val erikoisalat = erikoisalaTunnisteList.mapNotNull {
                erikoisalaRepository.findOneByVirtaPatevyyskoodi(it)
            }
            return erikoisalat.takeIf { it.size == 1 }?.first() ?: run {
                log.warn(
                    "$yliopisto, user id: $userId. Yhtäkään erikoisalaa ei löytynyt Elsan tietokannasta tai erikoisaloja " +
                        "oli enemmän kuin yksi per opinto-oikeus. Erikoisalat: $erikoisalaTunnisteList"
                )
                return null
            }
        }
    }

    private fun findLatestOpintoopasByErikoisalaOrLogError(erikoisalaId: Long): Opintoopas? {
        return opintoopasRepository.findAllByErikoisalaId(erikoisalaId)
            .maxByOrNull { it.voimassaoloAlkaa!! }
            ?: run {
                log.error("Viimeisintä opinto-opasta ei löytynyt. Erikoisala id: $erikoisalaId")
                return null
            }
    }

    private fun findOpintoopasByErikoisalaAndVoimassaDateOrLogWarn(
        erikoisalaId: Long,
        voimassaDate: LocalDate,
        yliopisto: YliopistoEnum,
        userId: String
    ): Opintoopas? {
        return opintoopasRepository.findOneByErikoisalaIdAndVoimassaDate(erikoisalaId, voimassaDate)
            ?: run {
                log.warn(
                    "$yliopisto, user id: $userId. Voimassaolevaa opinto-opasta " +
                        "ei löytynyt erikoisalalle $erikoisalaId. Opinto-oikeuden alkamispäivä: $voimassaDate"
                )
                return null
            }
    }

    private fun checkOpintooikeudenTilaValueExistsOrLogError(
        opintooikeudenTila: OpintooikeudenTila?, yliopisto: YliopistoEnum, userId: String
    ): OpintooikeudenTila? {
        return opintooikeudenTila ?: run {
            log.warn(
                "$yliopisto, user id: $userId. Opinto-oikeuden tilaa ei ole asetettu."
            )
            return null
        }
    }

    private fun checkSyntymaaikaValidDateExistsOrLogError(
        opintotietodataDTOs: List<OpintotietodataDTO>,
        etunimi: String,
        sukunimi: String
    ): LocalDate? {
        return opintotietodataDTOs.firstOrNull { it.syntymaaika != null }?.syntymaaika ?: run {
            log.error(
                "Erikoistuja: $etunimi $sukunimi. Kelvollista syntymäaikaa ei löytynyt yhdestäkään " +
                    "opintotietojärjestelmästä, jossa erikoistujalla on opinto-oikeus. Yliopisto(t): ${
                        opintotietodataDTOs.map { it.opintooikeudet ?: listOf() }.flatten()
                            .joinToString { it.yliopisto.toString() }
                    })"
            )
            return null
        }
    }
}
