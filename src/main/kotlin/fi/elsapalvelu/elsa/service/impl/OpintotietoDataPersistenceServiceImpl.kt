package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.OpintotietoDataPersistenceService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.OpintotietoDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
@Transactional
class OpintotietoDataPersistenceServiceImpl(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val yliopistoRepository: YliopistoRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val opintoopasRepository: OpintoopasRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val asetusRepository: AsetusRepository
) : OpintotietoDataPersistenceService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        etunimi: String,
        sukunimi: String,
        opintotietoDataDTO: OpintotietoDataDTO
    ) {
        val yliopistoNimi = opintotietoDataDTO.yliopisto.toString()
        val syntymaaika =
            getSyntymaaikaOrLogError(opintotietoDataDTO.syntymaaika, yliopistoNimi, etunimi, sukunimi) ?: return
        val erikoistuvaLaakari = createErikoistuvaLaakari(cipher, originalKey, hetu, etunimi, sukunimi, syntymaaika)
        val opiskelijatunnus = opintotietoDataDTO.opiskelijatunnus

        // Ei käsitellä opinto-oikeuksia, joiden päättymispäivä menneisyydessä. Mahdollisesti puuttuva opinto-oikeuden
        // päättymispäivä käsitellään samassa yhteydessä muiden tarkistusten kanssa.
        opintotietoDataDTO.opintooikeudet?.filter {
            it.opintooikeudenPaattymispaiva == null || !it.opintooikeudenPaattymispaiva!!.isBefore(
                LocalDate.now()
            )
        }
            ?.forEach {
                createOpintooikeus(it, yliopistoNimi, etunimi, sukunimi, opiskelijatunnus, erikoistuvaLaakari)
            }
    }

    override fun createOrUpdateIfChanged(
        userId: String, etunimi: String, sukunimi: String, opintotietoDataDTO: OpintotietoDataDTO
    ) {
        val yliopistoNimi = opintotietoDataDTO.yliopisto.toString()
        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
            ?: throw EntityNotFoundException("Erikoistuvaa lääkäriä ei löydy.")

        updateNimiIfChanged(erikoistuvaLaakari, etunimi, sukunimi)
        getSyntymaaikaOrLogError(
            opintotietoDataDTO.syntymaaika,
            yliopistoNimi,
            etunimi,
            sukunimi
        )?.takeIf { erikoistuvaLaakari.syntymaaika != it }?.let {
            erikoistuvaLaakari.syntymaaika = it
        }
        opintotietoDataDTO.opintooikeudet?.forEach { opintooikeusDTO ->
            val opintooikeusId =
                getOpintooikeusIdOrLogError(opintooikeusDTO, yliopistoNimi, etunimi, sukunimi) ?: return@forEach
            opintooikeusRepository.findOneByYliopistoOpintooikeusIdAndErikoistuvaLaakariId(
                opintooikeusId, erikoistuvaLaakari.id!!
            )?.let { opintooikeus ->
                getOpintooikeudenPaattymispaivaOrLogError(
                    opintooikeusDTO.opintooikeudenPaattymispaiva, yliopistoNimi, etunimi, sukunimi
                )?.takeIf { it != opintooikeus.opintooikeudenPaattymispaiva }?.let {
                    opintooikeus.opintooikeudenPaattymispaiva = opintooikeusDTO.opintooikeudenPaattymispaiva
                }

                getOpintooikeudenTilaOrLogError(
                    opintooikeusDTO.tila, yliopistoNimi, etunimi, sukunimi
                )?.takeIf { tila -> opintooikeus.tila != tila }?.let { tila ->
                    opintooikeus.tila = tila
                }

                opintooikeusDTO.asetus.takeIf { asetusStr -> asetusStr != null && opintooikeus.asetus?.nimi != asetusStr }
                    .let { asetusStr ->
                        getAsetusOrLogError(asetusStr, yliopistoNimi, etunimi, sukunimi)?.let { asetus ->
                            opintooikeus.asetus = asetus
                            handleAsetusUpdated(
                                opintooikeus,
                                opintooikeusDTO.opintooikeudenPaattymispaiva,
                                yliopistoNimi,
                                etunimi,
                                sukunimi
                            )
                        }
                    }
            } ?: createOpintooikeus(
                opintooikeusDTO,
                yliopistoNimi,
                etunimi,
                sukunimi,
                opintotietoDataDTO.opiskelijatunnus,
                erikoistuvaLaakari
            )
        }
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
        yliopistoNimi: String,
        etunimi: String,
        sukunimi: String
    ) {
        var opintoopas: Opintoopas? = null

        if (newOpintooikeudenPaattymispaiva == null) {
            opintoopas = findLatestOpintoopasByErikoisala(existingOpintooikeus.erikoisala?.id!!) ?: return
        } else if (existingOpintooikeus.erikoisala?.tyyppi == ErikoisalaTyyppi.LAAKETIEDE) {
            // Erikoistuvalle lääkärille linkitetään erikoistujalle opinto-opas, joka on ollut voimassa opinto-oikeuden
            // päättymispäivästä 10 vuotta taaksepäin
            val opintooppaanAlkamispaiva = newOpintooikeudenPaattymispaiva.minusYears(10).plusDays(1)
            opintoopas = opintoopasRepository.findOneByVoimassaoloAlkaa(opintooppaanAlkamispaiva)
        } else if (existingOpintooikeus.erikoisala?.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE) {
            // Erikoistuvalle hammaslääkärille linkitetään erikoistujalle opinto-opas, joka on ollut voimassa opinto-oikeuden
            // päättymispäivästä 6 vuotta taaksepäin
            val opintooppaanAlkamispaiva = newOpintooikeudenPaattymispaiva.minusYears(6).plusDays(1)
            opintoopas = opintoopasRepository.findOneByVoimassaoloAlkaa(opintooppaanAlkamispaiva)
        }

        opintoopas?.let {
            existingOpintooikeus.opintoopas = it
            existingOpintooikeus.osaamisenArvioinninOppaanPvm = it.voimassaoloAlkaa
        } ?: log.error(
            "${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Opinto-opasta ei löydetty asetusmuutoksen jälkeen. " +
                "Uusi opinto-oikeuden päättymispäivä: $newOpintooikeudenPaattymispaiva"
        )
    }

    override fun createWithoutOpintotietoData(
        cipher: Cipher, originalKey: SecretKey, hetu: String?, etunimi: String, sukunimi: String
    ) {
        val erikoistuvaLaakari = createErikoistuvaLaakari(
            cipher, originalKey, hetu, etunimi, sukunimi, LocalDate.now(ZoneId.systemDefault()).minusYears(40)
        )
        val yliopisto = yliopistoRepository.findByIdOrNull(1)
        val erikoisala = erikoisalaRepository.findByIdOrNull(46)
        val opintoopas = opintoopasRepository.findByIdOrNull(15)
        val asetus = asetusRepository.findByIdOrNull(5)
        val validOpintooikeusExists = opintooikeusRepository.existsByErikoistuvaLaakariKayttajaUserId(
            erikoistuvaLaakari.kayttaja?.user?.id!!, LocalDate.now()
        )
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
            kaytossa = !validOpintooikeusExists
        )
        opintooikeus = opintooikeusRepository.save(opintooikeus)

        erikoistuvaLaakari.opintooikeudet.add(opintooikeus)
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
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

        val existingAuthentication = SecurityContextHolder.getContext().authentication as Saml2Authentication
        SecurityContextHolder.getContext().authentication = Saml2Authentication(
            existingAuthentication.principal as AuthenticatedPrincipal?, existingAuthentication.saml2Response, listOf(
                SimpleGrantedAuthority(
                    ERIKOISTUVA_LAAKARI
                )
            )
        )

        return erikoistuvaLaakari
    }

    private fun createOpintooikeus(
        opintooikeusDTO: OpintotietoOpintooikeusDataDTO,
        yliopistoNimi: String,
        etunimi: String,
        sukunimi: String,
        opiskelijatunnus: String?,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ) {
        val opintooikeusId =
            getOpintooikeusIdOrLogError(opintooikeusDTO, yliopistoNimi, etunimi, sukunimi) ?: return
        val yliopisto = yliopistoRepository.findOneByNimi(yliopistoNimi)
        val asetus = getAsetusOrLogError(opintooikeusDTO.asetus, yliopistoNimi, etunimi, sukunimi) ?: return
        val opintooikeudenTila =
            getOpintooikeudenTilaOrLogError(opintooikeusDTO.tila, yliopistoNimi, etunimi, sukunimi) ?: return
        val opintooikeudenAlkamispaiva = getOpintooikeudenAlkamispaivaOrLogError(
            opintooikeusDTO.opintooikeudenAlkamispaiva, yliopistoNimi, etunimi, sukunimi
        ) ?: return
        val opintooikeudenPaattymispaiva = getOpintooikeudenPaattymispaivaOrLogError(
            opintooikeusDTO.opintooikeudenPaattymispaiva, yliopistoNimi, etunimi, sukunimi
        ) ?: return
        val opintoopas =
            getOpintoopasOrLogError(opintooikeudenAlkamispaiva, yliopistoNimi, etunimi, sukunimi) ?: return

        // TODO
        val erikoisala = erikoisalaRepository.findByIdOrNull(46)

        var opintooikeus = Opintooikeus(
            yliopistoOpintooikeusId = opintooikeusId,
            opintooikeudenMyontamispaiva = opintooikeudenAlkamispaiva,
            opintooikeudenPaattymispaiva = opintooikeudenPaattymispaiva,
            opiskelijatunnus = opiskelijatunnus,
            asetus = asetus,
            osaamisenArvioinninOppaanPvm = opintoopas.voimassaoloAlkaa,
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

    private fun getOpintooikeusIdOrLogError(
        opintooikeusDTO: OpintotietoOpintooikeusDataDTO, yliopistoNimi: String, etunimi: String, sukunimi: String
    ): String? = opintooikeusDTO.id ?: run {
        log.error("${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Opinto-oikeus id:tä ei ole asetettu.")
        return null
    }

    private fun getOpintooikeudenAlkamispaivaOrLogError(
        alkamispaiva: LocalDate?, yliopistoNimi: String, etunimi: String, sukunimi: String
    ): LocalDate? = alkamispaiva ?: run {
        log.error("${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Opinto-oikeuden alkamispäivää ei ole asetettu.")
        return null
    }

    private fun getOpintooikeudenPaattymispaivaOrLogError(
        paattymispaiva: LocalDate?, yliopistoNimi: String, etunimi: String, sukunimi: String
    ): LocalDate? = paattymispaiva ?: run {
        log.error("${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Opinto-oikeuden päättymispäivää ei ole asetettu.")
        return null
    }

    private fun getAsetusOrLogError(
        asetus: String?, yliopistoNimi: String, etunimi: String, sukunimi: String
    ): Asetus? {
        return asetus?.let { it ->
            asetusRepository.findOneByNimi(it) ?: run {
                log.error("${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Asetusta $it ei löydy ELSA:n tietokannasta.")
            }
        } as Asetus?
    }

    private fun getOpintoopasOrLogError(
        opintooikeudenAlkamispaiva: LocalDate, yliopistoNimi: String, etunimi: String, sukunimi: String
    ): Opintoopas? {
        return (opintoopasRepository.findOneByVoimassaoloAlkaa(opintooikeudenAlkamispaiva) ?: run {
            log.error(
                "${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Opinto-oikeuden alkamispäivän " + "$opintooikeudenAlkamispaiva mukaista opinto-opasta ei löydy ELSA:n tietokannasta."
            )
        }) as Opintoopas?
    }

    private fun getOpintooikeudenTilaOrLogError(
        opintooikeudenTila: OpintooikeudenTila?, yliopistoNimi: String, etunimi: String, sukunimi: String
    ): OpintooikeudenTila? {
        return (opintooikeudenTila ?: run {
            log.error(
                "${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Opinto-oikeuden tilaa ei ole asetettu."
            )
        }) as OpintooikeudenTila?
    }

    private fun getSyntymaaikaOrLogError(
        syntymaaika: String?,
        yliopistoNimi: String,
        etunimi: String,
        sukunimi: String
    ): LocalDate? {
        return (syntymaaika?.let { LocalDate.parse(it) } ?: run {
            log.error(
                "${yliopistoNimi}, erikoistuja: $etunimi $sukunimi. Syntymäaikaa ei ole asetettu tai se ei ole "
                    + "kelvollisessa muodossa."
            )
        }) as LocalDate?
    }

    private fun findLatestOpintoopasByErikoisala(erikoisalaId: Long): Opintoopas? =
        opintoopasRepository.findAllByErikoisalaId(erikoisalaId).maxByOrNull { it.voimassaoloAlkaa!! }
}
