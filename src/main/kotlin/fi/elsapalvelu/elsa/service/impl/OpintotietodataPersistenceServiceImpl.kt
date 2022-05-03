package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.periodLessThan
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
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
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

private const val SUU_JA_LEUKAKIRURGIA_VIRTA_PATEVYYSKOODI = "esl"

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
    private val opintooikeusHerateRepository: OpintooikeusHerateRepository,
    private val asetusRepository: AsetusRepository,
    private val erikoisalaSisuTutkintoohjelmaRepository: ErikoisalaSisuTutkintoohjelmaRepository,
    private val clock: Clock,
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
        val syntymaaika = opintotietodataDTOs.first().let {
            checkSyntymaaikaValidDateExistsOrLogError(
                it.syntymaaika,
                it.yliopisto,
                etunimi,
                sukunimi
            )
        } ?: return
        val erikoistuvaLaakari = createErikoistuvaLaakari(cipher, originalKey, hetu, etunimi, sukunimi, syntymaaika)
        val userId = erikoistuvaLaakari.kayttaja?.user?.id!!

        flattenAndfilterOpintooikeudetByVoimassaDate(opintotietodataDTOs)?.let {
            checkOpintooikeudetAmount(it, erikoistuvaLaakari, flattenYliopistot(opintotietodataDTOs))
        }

        opintotietodataDTOs.forEach { dto ->
            filterOpintooikeudetByVoimassaDate(dto.opintooikeudet)?.forEach {
                createOpintooikeus(it, dto.yliopisto, userId, dto.opiskelijatunnus, erikoistuvaLaakari)
            }
        }
    }

    override fun createOrUpdateIfChanged(
        userId: String, etunimi: String, sukunimi: String, opintotietodataDTOs: List<OpintotietodataDTO>
    ) {
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
            ?: throw EntityNotFoundException("Erikoistuvaa lääkäriä ei löydy.")

        updateNimiIfChanged(erikoistuvaLaakari, etunimi, sukunimi)

        flattenAndfilterOpintooikeudetByVoimassaDate(opintotietodataDTOs)?.let {
            checkOpintooikeudetAmount(it, erikoistuvaLaakari, flattenYliopistot(opintotietodataDTOs))
        }

        opintotietodataDTOs.forEach { dto ->
            filterOpintooikeudetByVoimassaDate(dto.opintooikeudet)?.forEach {
                createOrUpdateOpintooikeus(it, dto.yliopisto, userId, dto.opiskelijatunnus, erikoistuvaLaakari)
            }
        }
    }

    override fun createWithoutOpintotietodata(
        cipher: Cipher, originalKey: SecretKey, hetu: String?, etunimi: String, sukunimi: String
    ) {
        val erikoistuvaLaakari = createErikoistuvaLaakari(
            cipher, originalKey, hetu, etunimi, sukunimi, LocalDate.now(ZoneId.systemDefault()).minusYears(40)
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

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
    }

    private fun flattenAndfilterOpintooikeudetByVoimassaDate(opintotietodataDTOs: List<OpintotietodataDTO>): List<OpintotietoOpintooikeusDataDTO>? =
        opintotietodataDTOs.map { it.opintooikeudet ?: listOf() }.flatten().let {
            filterOpintooikeudetByVoimassaDate(it)
        }

    private fun filterOpintooikeudetByVoimassaDate(opintooikeudet: List<OpintotietoOpintooikeusDataDTO>?):
        List<OpintotietoOpintooikeusDataDTO>? =
        opintooikeudet?.filter {
            it.opintooikeudenPaattymispaiva == null || !it.opintooikeudenPaattymispaiva!!.isBefore(
                LocalDate.now(clock)
            )
        }

    private fun flattenYliopistot(opintotietodataDTOs: List<OpintotietodataDTO>): List<YliopistoEnum> =
        opintotietodataDTOs.map { it.yliopisto }

    private fun checkOpintooikeudetAmount(
        opintooikeudetVoimassa: List<OpintotietoOpintooikeusDataDTO>?,
        erikoistuvaLaakari: ErikoistuvaLaakari,
        yliopistot: List<YliopistoEnum>
    ) {
        val user = erikoistuvaLaakari.kayttaja?.user!!
        var opintooikeusHerate: OpintooikeusHerate? = null
        opintooikeudetVoimassa?.takeIf { it.size > 2 }?.takeIf {
            opintooikeusHerate = opintooikeusHerateRepository.findOneByErikoistuvaLaakariKayttajaUserId(user.id!!)
            opintooikeusHerate?.useaVoimassaolevaHerateLahetetty == null
        }?.let {
            mailService.sendEmailFromTemplate(
                user,
                getOpintohallintoEmailAddresses(yliopistot),
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

    private fun getOpintohallintoEmailAddresses(yliopistot: List<YliopistoEnum>): Array<String> {
        return yliopistot.mapNotNull { getOpintohallintoEmailAddress(it) }.toTypedArray()
    }

    private fun getOpintohallintoEmailAddress(yliopisto: YliopistoEnum): String? {
        return when (yliopisto) {
            YliopistoEnum.OULUN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().oulu
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().tre
            YliopistoEnum.TURUN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().turku
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> applicationProperties.getOpintohallintoemail().uef
            else -> null
        }
    }

    private fun createErikoistuvaLaakari(
        cipher: Cipher, originalKey: SecretKey, hetu: String?, etunimi: String, sukunimi: String, syntymaaika: LocalDate
    ): ErikoistuvaLaakari {
        val userDTO = userService.createUser(cipher, originalKey, hetu, etunimi, sukunimi)
        var user = userRepository.findById(userDTO.id!!).orElseThrow()

        user.authorities.add(Authority(name = ERIKOISTUVA_LAAKARI))
        user = userRepository.save(user)

        val kayttaja = kayttajaRepository.save(
            Kayttaja(
                user = user,
            )
        )

        var erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = kayttaja, syntymaaika = syntymaaika)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        return erikoistuvaLaakari
    }

    private fun createOpintooikeus(
        opintooikeusDTO: OpintotietoOpintooikeusDataDTO,
        yliopistoEnum: YliopistoEnum,
        userId: String,
        opiskelijatunnus: String?,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val opintooikeusId =
            checkOpintooikeusIdValueExistsOrLogError(opintooikeusDTO.id, yliopistoEnum, userId) ?: return
        val yliopisto = findYliopistoOrLogError(yliopistoEnum) ?: return
        val asetusStr =
            checkAsetusValueExistsOrLogError(opintooikeusDTO.asetus, yliopistoEnum, userId) ?: return
        val asetus = findAsetusOrLogError(asetusStr, yliopistoEnum, userId) ?: return
        val opintooikeudenTila =
            checkOpintooikeudenTilaValueExistsOrLogError(opintooikeusDTO.tila, yliopistoEnum, userId)
                ?: return
        val opintooikeudenAlkamispaiva = checkOpintooikeudenAlkamispaivaValidDateExistsOrLogError(
            opintooikeusDTO.opintooikeudenAlkamispaiva, yliopistoEnum, userId
        ) ?: return
        val opintooikeudenPaattymispaiva = checkOpintooikeudenPaattymispaivaValidDateExistsOrLogError(
            opintooikeusDTO.opintooikeudenPaattymispaiva, yliopistoEnum, userId
        ) ?: return
        val erikoisala =
            findErikoisalaOrLogError(
                opintooikeusDTO.erikoisalaTunniste,
                yliopistoEnum,
                userId
            ) ?: return
        val opintoopas =
            findOpintoopasByErikoisalaAndVoimassaDateOrLogWarn(
                erikoisala.id!!,
                opintooikeudenAlkamispaiva,
                yliopistoEnum,
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
            opiskelijatunnus = opiskelijatunnus,
            asetus = asetus,
            osaamisenArvioinninOppaanPvm = opintooikeudenAlkamispaiva,
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala,
            opintoopas = opintoopas,
            kaytossa = true,
            tila = opintooikeudenTila,
            muokkausaika = Instant.now()
        )
        opintooikeus = opintooikeusRepository.save(opintooikeus)

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
    }

    private fun createOrUpdateOpintooikeus(
        opintooikeusDTO: OpintotietoOpintooikeusDataDTO,
        yliopistoEnum: YliopistoEnum,
        userId: String,
        opiskelijatunnus: String?,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val opintooikeusId =
            checkOpintooikeusIdValueExistsOrLogError(opintooikeusDTO.id, yliopistoEnum, userId)
                ?: return
        val yliopisto = findYliopistoOrLogError(yliopistoEnum) ?: return
        val erikoisala = findErikoisalaOrLogError(
            opintooikeusDTO.erikoisalaTunniste,
            yliopistoEnum,
            userId
        ) ?: return

        findExistingOpintooikeus(
            opintooikeusId,
            erikoistuvaLaakari.id!!,
            yliopisto.id!!,
            erikoisala.id!!
        )?.also { opintooikeus ->
            val opintooikeudenTila = checkOpintooikeudenTilaValueExistsOrLogError(
                opintooikeusDTO.tila, yliopistoEnum, userId
            ) ?: return
            val asetusStr =
                checkAsetusValueExistsOrLogError(opintooikeusDTO.asetus, yliopistoEnum, userId)
                    ?: return

            opintooikeusId.takeIf { opintooikeus.yliopistoOpintooikeusId == null }
                ?.let { opintooikeus.yliopistoOpintooikeusId = it }

            opintooikeusDTO.opintooikeudenPaattymispaiva?.takeIf { it != opintooikeus.opintooikeudenPaattymispaiva }
                ?.let {
                    opintooikeus.opintooikeudenPaattymispaiva = it
                }

            opintooikeudenTila.takeIf { it != opintooikeus.tila }?.let {
                opintooikeus.tila = it
            }

            asetusStr.takeIf { it != opintooikeus.asetus?.nimi }?.let {
                findAsetusOrLogError(it, yliopistoEnum, userId)?.let { asetus ->
                    opintooikeus.asetus = asetus
                    handleAsetusUpdated(
                        opintooikeus,
                        opintooikeusDTO.opintooikeudenPaattymispaiva,
                        yliopistoEnum,
                        userId
                    )
                } ?: return
            }
            opintooikeus.muokkausaika = Instant.now()
        } ?: createOpintooikeus(
            opintooikeusDTO,
            yliopistoEnum,
            userId,
            opiskelijatunnus,
            erikoistuvaLaakari
        )
    }

    private fun findExistingOpintooikeus(
        opintooikeusId: String,
        erikoistuvaLaakariId: Long,
        yliopistoId: Long,
        erikoisalaId: Long
    ): Opintooikeus? {
        // Päivitetään olemassaolevaa opinto-oikeutta jos sellainen löytyy Sisun opinto-oikeuden id:llä
        // (haettu ja tallennettu aiemmin opintotietojärjestelmästä) tai erikoisalan ja yliopiston id:llä
        // (erikoistuva kutsuttu aiemmin teknisen pääkäyttäjän toimesta).
        return opintooikeusRepository.findOneByErikoistuvaLaakariIdAndYliopistoOpintooikeusId(
            erikoistuvaLaakariId, opintooikeusId
        )
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
                findLatestOpintoopasByErikoisalaOrLogError(existingOpintooikeus.erikoisala?.id!!) ?: return
            } else {
                val opintoopasValidDate =
                    newOpintooikeudenPaattymispaiva!!.minusYears(defaultOpintooikeudenPituus.toLong())
                findOpintoopasByErikoisalaAndVoimassaDateOrLogWarn(
                    existingOpintooikeus.erikoisala?.id!!,
                    opintoopasValidDate,
                    yliopisto,
                    userId
                ) ?: findLatestOpintoopasByErikoisalaOrLogError(existingOpintooikeus.erikoisala?.id!!) ?: return
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
        log.error("$yliopisto, user id: $userId. Opinto-oikeus id:tä ei ole asetettu.")
        return null
    }

    private fun checkOpintooikeudenAlkamispaivaValidDateExistsOrLogError(
        alkamispaiva: LocalDate?, yliopisto: YliopistoEnum, userId: String
    ): LocalDate? = alkamispaiva ?: run {
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
        erikoisalaTunniste: String?, yliopisto: YliopistoEnum, userId: String
    ): Erikoisala? {
        // TODO: Toteuta erikoisalan tunnistaminen yliopistokohtaisesti.
        return erikoisalaTunniste?.let {
            erikoisalaSisuTutkintoohjelmaRepository.findOneByTutkintoohjelmaId(
                erikoisalaTunniste
            )?.erikoisala
        } ?: run {
            log.error(
                "$yliopisto, user id: $userId. Erikoisalaa ei tunnistettu opintotietojärjestelmästä saadun " +
                    "tunnisteen $erikoisalaTunniste perusteella."
            )
            return null
        }
    }

    private fun findLatestOpintoopasByErikoisalaOrLogError(erikoisalaId: Long): Opintoopas? {
        return opintoopasRepository.findAllByErikoisalaId(erikoisalaId).maxByOrNull { it.voimassaoloAlkaa!! }
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
                    "$yliopisto, user id: $userId. $voimassaDate voimassaolevaa opinto-opasta " +
                        "ei löytynyt erikoisalalle $erikoisalaId"
                )
                return null
            }
    }

    private fun checkOpintooikeudenTilaValueExistsOrLogError(
        opintooikeudenTila: OpintooikeudenTila?, yliopisto: YliopistoEnum, userId: String
    ): OpintooikeudenTila? {
        return opintooikeudenTila ?: run {
            log.error(
                "$yliopisto, user id: $userId. Opinto-oikeuden tilaa ei ole asetettu."
            )
            return null
        }
    }

    private fun checkSyntymaaikaValidDateExistsOrLogError(
        syntymaaika: String?,
        yliopisto: YliopistoEnum,
        etunimi: String,
        sukunimi: String
    ): LocalDate? {
        return syntymaaika?.let { LocalDate.parse(it) } ?: run {
            log.error(
                "$yliopisto, erikoistuja: $etunimi $sukunimi. Syntymäaikaa ei ole asetettu tai se ei ole "
                    + "kelvollisessa muodossa."
            )
            return null
        }
    }
}
