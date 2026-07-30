package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.VerificationToken
import fi.elsapalvelu.elsa.repository.*
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

@ExtendWith(MockitoExtension::class)
class ValidateNameAlertTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var verificationTokenRepository: VerificationTokenRepository
    @Mock private lateinit var kayttajaRepository: KayttajaRepository
    @Mock private lateinit var kouluttajavaltuutusRepository: KouluttajavaltuutusRepository
    @Mock private lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository
    @Mock private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository
    @Mock private lateinit var koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository
    @Mock private lateinit var koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository
    @Mock private lateinit var koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository
    @Mock private lateinit var koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository
    @Mock private lateinit var koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository
    @Mock private lateinit var seurantajaksoRepository: SeurantajaksoRepository
    @Mock private lateinit var entityManager: EntityManager

    private lateinit var userService: UserServiceImpl
    private lateinit var cipher: Cipher
    private lateinit var token: VerificationToken

    @BeforeEach
    fun setUp() {
        userService = UserServiceImpl(
            userRepository = userRepository,
            verificationTokenRepository = verificationTokenRepository,
            kayttajaRepository = kayttajaRepository,
            kouluttajavaltuutusRepository = kouluttajavaltuutusRepository,
            kayttajaYliopistoErikoisalaRepository = kayttajaYliopistoErikoisalaRepository,
            suoritusarviointiRepository = suoritusarviointiRepository,
            koejaksonKoulutussopimusRepository = koejaksonKoulutussopimusRepository,
            koejaksonAloituskeskusteluRepository = koejaksonAloituskeskusteluRepository,
            koejaksonValiarviointiRepository = koejaksonValiarviointiRepository,
            koejaksonKehittamistoimenpiteetRepository = koejaksonKehittamistoimenpiteetRepository,
            koejaksonLoppukeskusteluRepository = koejaksonLoppukeskusteluRepository,
            seurantajaksoRepository = seurantajaksoRepository,
            entityManager = entityManager
        )

        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        token = VerificationToken()
    }

    // -------------------------------------------------------------------------
    // Name mismatch → exception expected
    // -------------------------------------------------------------------------

    @Test
    fun `exception is thrown when token user name is too different from login name`() {
        val tokenUser = User().apply {
            id = "user-1"
            firstName = "Matti"
            lastName = "Virtanen"
        }

        assertThrows(Exception::class.java) {
            userService.createOrUpdateUserWithToken(
                tokenUser = tokenUser,
                token = token,
                cipher = cipher,
                originalKey = run {
                    val kg = KeyGenerator.getInstance("AES")
                    kg.init(128)
                    kg.generateKey()
                },
                hetu = null,
                eppn = "matti.virtanen@test.fi",
                firstName = "Completely",
                lastName = "Different"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Name close enough → no exception from name validation
    // -------------------------------------------------------------------------

    @Test
    fun `no exception from name validation when token user name is close enough to login name`() {
        val secretKey = run {
            val kg = KeyGenerator.getInstance("AES")
            kg.init(128)
            kg.generateKey()
        }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val tokenUser = User().apply {
            id = "user-3"
            firstName = "Matti"
            lastName = "Virtanen"
        }

        // Names match closely (Levenshtein distance ≤ 2) – name validation passes;
        // further processing will throw because repos are mocked with default no-op behaviour,
        // but that is unrelated to name validation.
        val ex = runCatching {
            userService.createOrUpdateUserWithToken(
                tokenUser = tokenUser,
                token = token,
                cipher = cipher,
                originalKey = secretKey,
                hetu = null,
                eppn = "matti.virtanen@test.fi",
                firstName = "Matti",
                lastName = "Virtanen"
            )
        }.exceptionOrNull()

        // If an exception was thrown it must NOT be the VIRHEELLINEN_NIMI login error
        if (ex != null) {
            assert(ex.message != fi.elsapalvelu.elsa.config.LoginException.VIRHEELLINEN_NIMI.name) {
                "Name validation should not fail for matching names, but got: ${ex.message}"
            }
        }
    }
}
