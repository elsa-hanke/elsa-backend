package fi.elsapalvelu.elsa.service.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.VerificationToken
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.AlertPublisherService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
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
    @Mock private lateinit var alertPublisherService: AlertPublisherService

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
            entityManager = entityManager,
            alertPublisherService = alertPublisherService
        )

        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128)
        val secretKey = keyGenerator.generateKey()
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        token = VerificationToken()
    }

    // -------------------------------------------------------------------------
    // Name mismatch → alert expected
    // -------------------------------------------------------------------------

    @Test
    fun `alert is published when token user name is too different from login name`() {
        val tokenUser = User().apply {
            id = "user-1"
            firstName = "Matti"
            lastName = "Virtanen"
        }

        // "Completely Different" is far from "Matti Virtanen" → validation should fail
        assertThrows(Exception::class.java) {
            userService.createOrUpdateUserWithToken(
                tokenUser = tokenUser,
                token = token,
                cipher = cipher,
                originalKey = cipher.parameters?.let { null } ?: run {
                    val kg = KeyGenerator.getInstance("AES")
                    kg.init(128)
                    kg.generateKey()
                }!!,
                hetu = null,
                eppn = "matti.virtanen@test.fi",
                firstName = "Completely",
                lastName = "Different"
            )
        }

        val subjectCaptor = argumentCaptor<String>()
        val messageCaptor = argumentCaptor<String>()
        verify(alertPublisherService).publishAlert(subjectCaptor.capture(), messageCaptor.capture())

        assertThat(subjectCaptor.firstValue).contains("virheellinen nimi")
        assertThat(messageCaptor.firstValue).contains("Completely")
        assertThat(messageCaptor.firstValue).contains("Different")
        assertThat(messageCaptor.firstValue).contains("matti.virtanen@test.fi")
    }

    @Test
    fun `alert message contains token user name`() {
        val tokenUser = User().apply {
            id = "user-2"
            firstName = "Pekka"
            lastName = "Korhonen"
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
                eppn = "pekka.korhonen@test.fi",
                firstName = "Xyz",
                lastName = "Abc"
            )
        }

        val messageCaptor = argumentCaptor<String>()
        verify(alertPublisherService).publishAlert(any(), messageCaptor.capture())

        assertThat(messageCaptor.firstValue).contains("Pekka")
        assertThat(messageCaptor.firstValue).contains("Korhonen")
    }

    // -------------------------------------------------------------------------
    // Name close enough → no alert expected
    // -------------------------------------------------------------------------

    @Test
    fun `no alert is published when token user name is close enough to login name`() {
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

        // Names match closely (Levenshtein distance ≤ 2) – validation passes
        // but further processing will throw because kayttajaRepository is a mock
        // returning empty; we only care that no alert was published.
        try {
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
        } catch (_: Exception) {
            // expected – repos are mocked with default no-op / empty behaviour
        }

        verify(alertPublisherService, never()).publishAlert(any(), any())
    }
}

