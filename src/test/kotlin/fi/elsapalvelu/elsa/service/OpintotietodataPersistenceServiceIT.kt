package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintoopasHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@SpringBootTest(classes = [ElsaBackendApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class OpintotietodataPersistenceServiceIT {

    @Autowired
    private lateinit var opintotietodataPersistenceService: OpintotietodataPersistenceService

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var opintooikeusRepository: OpintooikeusRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var yliopistoRepository: YliopistoRepository

    private lateinit var erikoisala: Erikoisala

    private lateinit var secondErikoisala: Erikoisala

    private lateinit var opintoopas: Opintoopas

    private lateinit var asetus: Asetus

    private lateinit var secondAsetus: Asetus

    private lateinit var originalKey: SecretKey

    private lateinit var cipher: Cipher

    // 5 vuotta = 31.12.1974
    private val now = 157680000L

    @MockBean
    private lateinit var clock: Clock

    @BeforeAll
    fun setupForAll() {
        val decodedKey = Base64.getDecoder().decode("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5WFk")
        originalKey = SecretKeySpec(
            decodedKey, 0, decodedKey.size, "AES"
        )
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    }

    @BeforeEach
    fun setupForEach() {
        `when`(clock.instant()).thenReturn(Instant.ofEpochSecond(now))
        `when`(clock.zone).thenReturn(ZoneId.systemDefault())

        em.persist(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO.toString()))
        em.persist(Yliopisto(nimi = YliopistoEnum.OULUN_YLIOPISTO.toString()))
        em.persist(Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO.toString()))
        em.persist(Yliopisto(nimi = YliopistoEnum.TURUN_YLIOPISTO.toString()))
        em.persist(Yliopisto(nimi = YliopistoEnum.ITA_SUOMEN_YLIOPISTO.toString()))

        asetus = Asetus(nimi = asetusNimi)
        em.persist(asetus)

        secondAsetus = Asetus(nimi = secondAsetusNimi)
        em.persist(secondAsetus)

        erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        secondErikoisala = ErikoisalaHelper.createEntity()
        em.persist(secondErikoisala)

        setupDegreeProgrammeIdsForHYSisu()

        // Luodaan molemmille erikoisaloille kaksi opinto-opasta.
        opintoopas =
            OpintoopasHelper.createEntity(
                em,
                defaultOpintopasVoimassaoloAlkaa,
                defaultOpintopasVoimassaoloPaattyy,
                erikoisala
            )
        em.persist(opintoopas)

        em.persist(
            OpintoopasHelper.createEntity(
                em,
                defaultLatestOpintopasVoimassaoloAlkaa,
                null,
                erikoisala
            )
        )

        em.persist(
            OpintoopasHelper.createEntity(
                em,
                defaultOpintopasVoimassaoloAlkaa,
                defaultOpintopasVoimassaoloPaattyy,
                secondErikoisala
            )
        )

        em.persist(
            OpintoopasHelper.createEntity(
                em,
                defaultLatestOpintopasVoimassaoloAlkaa,
                null,
                secondErikoisala
            )
        )

        em.flush()
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldPersistOpintotietodata(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData())
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(existingUser.id!!)
        assertThat(erikoistuvaLaakari?.syntymaaika).isEqualTo(syntymaaika)

        val opintooikeus = opintooikeudet.first()
        assertThat(opintooikeus.erikoistuvaLaakari).isNotNull
        assertThat(opintooikeus.erikoistuvaLaakari!!.id).isEqualTo(erikoistuvaLaakari!!.id)
        assertThat(opintooikeus.yliopistoOpintooikeusId).isEqualTo(opintooikeusId)
        assertThat(opintooikeus.yliopisto?.nimi).isEqualTo(yliopisto.toString())
        assertThat(opintooikeus.opiskelijatunnus).isEqualTo(opiskelijatunnus)
        assertThat(opintooikeus.erikoisala).isEqualTo(erikoisala)
        assertThat(opintooikeus.opintooikeudenMyontamispaiva).isEqualTo(defaultOpintooikeudenMyontamispaiva)
        assertThat(opintooikeus.opintooikeudenPaattymispaiva).isEqualTo(defaultOpintooikeudenPaattymispaiva)
        assertThat(opintooikeus.asetus).isEqualTo(asetus)
        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isEqualTo(defaultOpintopasVoimassaoloPaattyy)
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.kaytossa).isEqualTo(true)
        assertThat(opintooikeus.muokkausaika).isNotNull
        assertThat(opintooikeus.tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldPersistOpintotietodataWithMultipleOpintooikeus(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData(), createSecondOpintooikeusData())
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(2)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(existingUser.id!!)
        assertThat(erikoistuvaLaakari?.syntymaaika).isEqualTo(syntymaaika)

        val firstOpintooikeus = opintooikeudet[0]
        assertThat(firstOpintooikeus.erikoistuvaLaakari).isNotNull
        assertThat(firstOpintooikeus.erikoistuvaLaakari!!.id).isEqualTo(erikoistuvaLaakari!!.id)
        assertThat(firstOpintooikeus.yliopistoOpintooikeusId).isEqualTo(opintooikeusId)
        assertThat(firstOpintooikeus.yliopisto?.nimi).isEqualTo(yliopisto.toString())
        assertThat(firstOpintooikeus.opiskelijatunnus).isEqualTo(opiskelijatunnus)
        assertThat(firstOpintooikeus.erikoisala).isEqualTo(erikoisala)
        assertThat(firstOpintooikeus.opintooikeudenMyontamispaiva).isEqualTo(defaultOpintooikeudenMyontamispaiva)
        assertThat(firstOpintooikeus.opintooikeudenPaattymispaiva).isEqualTo(defaultOpintooikeudenPaattymispaiva)
        assertThat(firstOpintooikeus.asetus).isEqualTo(asetus)
        assertThat(firstOpintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultOpintopasVoimassaoloAlkaa)
        assertThat(firstOpintooikeus.opintoopas?.voimassaoloPaattyy).isEqualTo(defaultOpintopasVoimassaoloPaattyy)
        assertThat(firstOpintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultOpintopasVoimassaoloAlkaa)
        assertThat(firstOpintooikeus.kaytossa).isEqualTo(false)
        assertThat(firstOpintooikeus.muokkausaika).isNotNull
        assertThat(firstOpintooikeus.tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)

        val secondOpintooikeus = opintooikeudet[1]
        assertThat(secondOpintooikeus.yliopistoOpintooikeusId).isEqualTo(secondOpintooikeusId)
        assertThat(secondOpintooikeus.yliopisto?.nimi).isEqualTo(yliopisto.toString())
        assertThat(secondOpintooikeus.opiskelijatunnus).isEqualTo(opiskelijatunnus)
        assertThat(secondOpintooikeus.erikoisala).isEqualTo(secondErikoisala)
        assertThat(secondOpintooikeus.opintooikeudenMyontamispaiva).isEqualTo(defaultSecondOpintooikeudenMyontamispaiva)
        assertThat(secondOpintooikeus.opintooikeudenPaattymispaiva).isEqualTo(defaultSecondOpintooikeudenPaattymispaiva)
        assertThat(secondOpintooikeus.asetus).isEqualTo(secondAsetus)
        assertThat(secondOpintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(secondOpintooikeus.opintoopas?.voimassaoloPaattyy).isNull()
        assertThat(secondOpintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(secondOpintooikeus.kaytossa).isEqualTo(true)
        assertThat(secondOpintooikeus.muokkausaika).isNotNull
        assertThat(secondOpintooikeus.tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotPersistOpintotietodataWithMissingOpintooikeusId(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply { id = null })
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotPersistOpintotietodataWithMissingAsetus(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply { asetus = null })
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotPersistOpintotietodataWithMissingErikoisalaTunniste(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply { erikoisalaTunniste = null })
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotPersistOpintotietodataWithMissingOpintooikeudenAlkamispaiva(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply { opintooikeudenAlkamispaiva = null })
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotPersistOpintotietodataWithMissingOpintooikeudenPaattymispaiva(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply { opintooikeudenPaattymispaiva = null })
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotPersistOpintotietodataWithMissingOpintooikeudenTila(yliopisto: YliopistoEnum) {
        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply { tila = null })
        )

        opintotietodataPersistenceService.create(cipher, originalKey, hetu, etunimi, sukunimi, opintotietodataDTO)

        val existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        assertNotNull(existingUser)
        assertUserProperties(existingUser)

        val opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(existingUser.id!!)
        assertThat(opintooikeudet).size().isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldUpdateExistingOpintooikeusWithSameYliopistoAndErikoisala(yliopisto: YliopistoEnum) {
        val userId = initUserWithOpintooikeus(yliopistoEnum = yliopisto)
        val newOpintooikeudenPaattymispaiva = defaultOpintooikeudenPaattymispaiva.plusYears(10)

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(
                createOpintooikeusData().apply {
                    opintooikeudenPaattymispaiva = newOpintooikeudenPaattymispaiva
                }
            )
        )

        var opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        assertThat(erikoistuvaLaakari?.kayttaja?.user?.firstName).isEqualTo(etunimi)
        assertThat(erikoistuvaLaakari?.kayttaja?.user?.lastName).isEqualTo(sukunimi)

        val opintooikeus = opintooikeudet[0]
        assertThat(opintooikeus.yliopistoOpintooikeusId).isEqualTo(opintooikeusId)
        assertThat(opintooikeus.opintooikeudenPaattymispaiva).isEqualTo(newOpintooikeudenPaattymispaiva)
        assertThat(opintooikeus.asetus).isEqualTo(asetus)
        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isNull()
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldUpdateExistingOpintooikeusWithSameOpintooikeusId(yliopisto: YliopistoEnum) {
        val userId = initUserWithOpintooikeus(opintooikeusId = opintooikeusId, yliopistoEnum = yliopisto)
        val newOpintooikeudenPaattymispaiva = defaultOpintooikeudenPaattymispaiva.plusYears(10)

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply {
                tila = OpintooikeudenTila.PASSIIVINEN
                opintooikeudenPaattymispaiva = newOpintooikeudenPaattymispaiva
            })
        )

        var opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val opintooikeus = opintooikeudet[0]
        assertThat(opintooikeus.opintooikeudenPaattymispaiva).isEqualTo(newOpintooikeudenPaattymispaiva)
        assertThat(opintooikeus.asetus).isEqualTo(asetus)
        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isNull()
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.tila).isEqualTo(OpintooikeudenTila.PASSIIVINEN)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldUseLatestOpintoopasWithAsetusUpdatedIfOpintooikeusLengthLessThanDefault(yliopisto: YliopistoEnum) {
        val paattymispaivaForTemporaryOpintooikeus = defaultOpintooikeudenMyontamispaiva.plusYears(2)
        // Alusta opinto-oikeuden pituuus määräaikaiseksi = 2 vuotta
        val userId = initUserWithOpintooikeus(
            opintooikeusId,
            defaultOpintooikeudenMyontamispaiva,
            paattymispaivaForTemporaryOpintooikeus,
            yliopisto
        )

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply {
                opintooikeudenPaattymispaiva = paattymispaivaForTemporaryOpintooikeus
            })
        )

        var opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val opintooikeus = opintooikeudet[0]
        assertThat(opintooikeus.asetus).isEqualTo(asetus)
        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isNull()
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldUseLatestOpintoopasWithAsetusUpdatedIfOpintooikeudenPaattymispaivaIsNull(yliopisto: YliopistoEnum) {
        val userId = initUserWithOpintooikeus(opintooikeusId = opintooikeusId, yliopistoEnum = yliopisto)

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createOpintooikeusData().apply {
                opintooikeudenPaattymispaiva = null
            })
        )

        var opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        opintooikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val opintooikeus = opintooikeudet[0]
        assertThat(opintooikeus.asetus).isEqualTo(asetus)
        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isNull()
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldCreateNewOpintooikeusIfAnotherWithSameIdOrYliopistoAndErikoisalaDoesNotExist(yliopisto: YliopistoEnum) {
        val userId = initUserWithOpintooikeus(yliopistoEnum = yliopisto)

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createSecondOpintooikeusData())
        )

        var opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(2)

        val opintooikeus = opintooikeudet[1]
        assertThat(opintooikeus.opintooikeudenMyontamispaiva).isEqualTo(defaultSecondOpintooikeudenMyontamispaiva)
        assertThat(opintooikeus.opintooikeudenPaattymispaiva).isEqualTo(defaultSecondOpintooikeudenPaattymispaiva)
        assertThat(opintooikeus.asetus).isEqualTo(secondAsetus)

        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isNull()
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultLatestOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.kaytossa).isEqualTo(true)
        assertThat(opintooikeus.muokkausaika).isNotNull
        assertThat(opintooikeus.tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotUpdateExistingOpintooikeusIfTilaIsNull(yliopisto: YliopistoEnum) {
        val userId = initUserWithOpintooikeus(opintooikeusId = opintooikeusId, yliopistoEnum = yliopisto)

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createSecondOpintooikeusData().apply { tila = null })
        )

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        val opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val opintooikeus = opintooikeudet[0]
        assertOpintooikeusDataNotUpdated(opintooikeus)
    }

    @ParameterizedTest
    @EnumSource(YliopistoEnum::class)
    @Transactional
    fun shouldNotUpdateExistingOpintooikeusIfAsetusIsNull(yliopisto: YliopistoEnum) {
        val userId = initUserWithOpintooikeus(opintooikeusId = opintooikeusId, yliopistoEnum = yliopisto)

        val opintotietodataDTO = OpintotietodataDTO(
            yliopisto,
            opiskelijatunnus,
            syntymaaika,
            opintooikeudet = listOf(createSecondOpintooikeusData().apply { asetus = null })
        )

        opintotietodataPersistenceService.createOrUpdateIfChanged(userId, etunimi, sukunimi, opintotietodataDTO)

        val opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        assertThat(opintooikeudet).size().isEqualTo(1)

        val opintooikeus = opintooikeudet[0]
        assertOpintooikeusDataNotUpdated(opintooikeus)
    }

    private fun initUserWithOpintooikeus(
        opintooikeusId: String? = null,
        opintooikeudenMyontamispaiva: LocalDate? = null,
        opintooikeudenPaattymispaiva: LocalDate? = null,
        yliopistoEnum: YliopistoEnum
    ): String {
        val yliopisto = yliopistoRepository.findOneByNimi(yliopistoEnum.toString())
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(
            em,
            null,
            opintooikeudenMyontamispaiva ?: defaultOpintooikeudenMyontamispaiva,
            opintooikeudenPaattymispaiva ?: defaultOpintooikeudenPaattymispaiva,
            erikoisala,
            opintoopas,
            yliopisto
        )
        val opintooikeus = erikoistuvaLaakari.opintooikeudet.first()
        opintooikeus.opintoopas = opintoopas
        opintooikeus.yliopistoOpintooikeusId = opintooikeusId
        opintooikeus.osaamisenArvioinninOppaanPvm = opintoopas.voimassaoloAlkaa

        return erikoistuvaLaakari.kayttaja?.user?.id!!
    }

    private fun setupDegreeProgrammeIdsForHYSisu() {
        em.persist(ErikoisalaSisuTutkintoohjelma(tutkintoohjelmaId = tutkintoohjelmaId, erikoisala = erikoisala))

        em.persist(
            ErikoisalaSisuTutkintoohjelma(
                tutkintoohjelmaId = secondTutkintoohjelmaId,
                erikoisala = secondErikoisala
            )
        )
    }

    private fun assertUserProperties(existingUser: User) {
        assertThat(existingUser.firstName).isEqualTo(etunimi)
        assertThat(existingUser.lastName).isEqualTo(sukunimi)
        assertThat(existingUser.hetu).isNotNull
        assertThat(existingUser.authorities).contains(Authority(ERIKOISTUVA_LAAKARI))
        assertThat(existingUser.activated).isEqualTo(true)
    }

    private fun assertOpintooikeusDataNotUpdated(opintooikeus: Opintooikeus) {
        assertThat(opintooikeus.opintooikeudenMyontamispaiva).isEqualTo(defaultOpintooikeudenMyontamispaiva)
        assertThat(opintooikeus.opintooikeudenPaattymispaiva).isEqualTo(defaultOpintooikeudenPaattymispaiva)
        assertThat(opintooikeus.asetus?.nimi).isEqualTo(em.findAll(Asetus::class).get(0).nimi)
        assertThat(opintooikeus.opintoopas?.voimassaoloAlkaa).isEqualTo(defaultOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.opintoopas?.voimassaoloPaattyy).isEqualTo(defaultOpintopasVoimassaoloPaattyy)
        assertThat(opintooikeus.osaamisenArvioinninOppaanPvm).isEqualTo(defaultOpintopasVoimassaoloAlkaa)
        assertThat(opintooikeus.tila).isEqualTo(OpintooikeudenTila.AKTIIVINEN)
    }

    companion object {

        private const val hetu = "123456-7890"

        private const val etunimi = "Test"

        private const val sukunimi = "User"

        private const val opiskelijatunnus = "123456"

        private const val syntymaaika = "1980-01-01"

        private const val opintooikeusId = "int-test-1"

        private const val secondOpintooikeusId = "int-test-2"

        // Ensimmäisen opinto-oikeuden pituus 10 vuotta.
        private val defaultOpintooikeudenMyontamispaiva = LocalDate.ofEpochDay(0L)
        private val defaultOpintooikeudenPaattymispaiva = LocalDate.ofEpochDay(3652L)

        // Toisen opinto-oikeuden pituus 6 vuotta.
        private val defaultSecondOpintooikeudenMyontamispaiva = LocalDate.ofEpochDay(25L)
        private val defaultSecondOpintooikeudenPaattymispaiva = LocalDate.ofEpochDay(2216L)

        private val defaultOpintopasVoimassaoloAlkaa = LocalDate.ofEpochDay(0L)

        private val defaultOpintopasVoimassaoloPaattyy = LocalDate.ofEpochDay(20L)

        private val defaultLatestOpintopasVoimassaoloAlkaa = LocalDate.ofEpochDay(21L)

        private const val asetusNimi = "01/test"

        private const val secondAsetusNimi = "02/test"

        private const val tutkintoohjelmaId = "int-test-DP-1"

        private const val secondTutkintoohjelmaId = "int-test-DP-2"

        @JvmStatic
        fun createOpintooikeusData(): OpintotietoOpintooikeusDataDTO {
            return OpintotietoOpintooikeusDataDTO(
                opintooikeusId,
                defaultOpintooikeudenMyontamispaiva,
                defaultOpintooikeudenPaattymispaiva,
                asetusNimi,
                tutkintoohjelmaId,
                OpintooikeudenTila.AKTIIVINEN
            )
        }

        @JvmStatic
        fun createSecondOpintooikeusData(): OpintotietoOpintooikeusDataDTO {
            return OpintotietoOpintooikeusDataDTO(
                secondOpintooikeusId,
                defaultSecondOpintooikeudenMyontamispaiva,
                defaultSecondOpintooikeudenPaattymispaiva,
                secondAsetusNimi,
                secondTutkintoohjelmaId,
                OpintooikeudenTila.AKTIIVINEN
            )
        }
    }

}