package fi.elsapalvelu.elsa.service.impl

import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.repository.koejakso.*
import fi.elsapalvelu.elsa.repository.tyoskentely.*
import fi.elsapalvelu.elsa.repository.arviointi.*
import fi.elsapalvelu.elsa.repository.suoritteet.*
import fi.elsapalvelu.elsa.repository.koulutus.*
import fi.elsapalvelu.elsa.repository.seuranta.*
import fi.elsapalvelu.elsa.repository.valmistuminen.*
import fi.elsapalvelu.elsa.repository.kayttaja.*
import fi.elsapalvelu.elsa.repository.perustiedot.*
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.env.Environment
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_PRODUCTION
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import javax.crypto.Cipher
import javax.crypto.SecretKey

import fi.elsapalvelu.elsa.service.impl.koulutus.OpintotietodataPersistenceServiceImpl
class OpintotietodataPersistenceServiceImplTest {

    @Mock private lateinit var userService: UserService
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
    @Mock private lateinit var kayttajaRepository: KayttajaRepository
    @Mock private lateinit var yliopistoRepository: YliopistoRepository
    @Mock private lateinit var erikoisalaRepository: ErikoisalaRepository
    @Mock private lateinit var opintoopasRepository: OpintoopasRepository
    @Mock private lateinit var opintooikeusRepository: OpintooikeusRepository
    @Mock private lateinit var asetusRepository: AsetusRepository
    @Mock private lateinit var erikoisalaSisuTutkintoohjelmaRepository: ErikoisalaSisuTutkintoohjelmaRepository
    @Mock private lateinit var opintooikeusHerateRepository: OpintooikeusHerateRepository
    @Mock private lateinit var mailService: MailService
    @Mock private lateinit var applicationProperties: ApplicationProperties
    @Mock private lateinit var env: Environment
    @Mock private lateinit var cipher: Cipher
    @Mock private lateinit var originalKey: SecretKey

    private val clock: Clock = Clock.fixed(
        LocalDate.of(2024, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    )

    private lateinit var service: OpintotietodataPersistenceServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = OpintotietodataPersistenceServiceImpl(
            userService,
            userRepository,
            erikoistuvaLaakariRepository,
            kayttajaRepository,
            yliopistoRepository,
            erikoisalaRepository,
            opintoopasRepository,
            opintooikeusRepository,
            asetusRepository,
            erikoisalaSisuTutkintoohjelmaRepository,
            clock,
            opintooikeusHerateRepository,
            mailService,
            applicationProperties,
            env
        )
    }

    @Test
    fun `createWithoutOpintotietodataOnlyDevDoNotUseInProd throws IllegalStateException when not in dev profile`() {
        whenever(env.activeProfiles).thenReturn(arrayOf(SPRING_PROFILE_PRODUCTION))

        assertThrows(IllegalStateException::class.java) {
            service.createWithoutOpintotietodataOnlyDevDoNotUseInProd(
                cipher, originalKey, "010190-1234", "Testi", "Henkilö"
            )
        }
    }

    @Test
    fun `createWithoutOpintotietodataOnlyDevDoNotUseInProd does not throw when in dev profile`() {
        whenever(env.activeProfiles).thenReturn(arrayOf(SPRING_PROFILE_DEVELOPMENT))

        val userId = "test-user-id"
        val user = stubUser(userId)
        val userDTO = UserDTO(id = userId, login = "testi.henkilo")

        whenever(userService.createUser(any(), any(), any(), any(), any())).thenReturn(userDTO)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val kayttaja = Kayttaja(user = user, tila = KayttajatilinTila.AKTIIVINEN)
        whenever(kayttajaRepository.save(any<Kayttaja>())).thenReturn(kayttaja)

        val erikoistuvaLaakari = ErikoistuvaLaakari(
            kayttaja = kayttaja,
            syntymaaika = LocalDate.now(ZoneId.systemDefault()).minusYears(40)
        )
        whenever(erikoistuvaLaakariRepository.save(any<ErikoistuvaLaakari>())).thenReturn(erikoistuvaLaakari)

        val erikoisala = Erikoisala(id = 46L, nimi = "Testala", tyyppi = ErikoisalaTyyppi.LAAKETIEDE)
        val yliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        val opintoopas = Opintoopas(id = 15L, erikoisala = erikoisala)
        val asetus = Asetus(id = 5L, nimi = "Asetus 2020")

        whenever(yliopistoRepository.findById(1)).thenReturn(Optional.of(yliopisto))
        whenever(erikoisalaRepository.findById(46)).thenReturn(Optional.of(erikoisala))
        whenever(opintoopasRepository.findById(15)).thenReturn(Optional.of(opintoopas))
        whenever(asetusRepository.findById(5)).thenReturn(Optional.of(asetus))

        val savedOpintooikeus = Opintooikeus(
            id = 1L,
            opintooikeudenMyontamispaiva = LocalDate.now(ZoneId.systemDefault()),
            opintooikeudenPaattymispaiva = LocalDate.now(ZoneId.systemDefault()).plusYears(10),
            erikoistuvaLaakari = erikoistuvaLaakari,
            yliopisto = yliopisto,
            erikoisala = erikoisala,
            opintoopas = opintoopas,
            asetus = asetus,
            tila = OpintooikeudenTila.AKTIIVINEN
        )
        whenever(opintooikeusRepository.save(any<Opintooikeus>())).thenReturn(savedOpintooikeus)
        whenever(userRepository.save(any<User>())).thenReturn(user)

        assertDoesNotThrow {
            service.createWithoutOpintotietodataOnlyDevDoNotUseInProd(
                cipher, originalKey, "010190-1234", "Testi", "Henkilö"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun stubUser(id: String): User = User(
        id = id,
        login = "testi.henkilo",
        firstName = "Testi",
        lastName = "Henkilö",
        authorities = mutableSetOf()
    )
}
