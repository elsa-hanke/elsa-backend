package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaYliopistoErikoisalaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKehittamistoimenpiteetRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonLoppukeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.service.AvatarValidationResult
import fi.elsapalvelu.elsa.service.AvatarValidator
import fi.elsapalvelu.elsa.service.dto.kayttaja.OmatTiedotDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceImplAvatarTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var verificationTokenRepository: VerificationTokenRepository

    @Mock
    private lateinit var kayttajaRepository: KayttajaRepository

    @Mock
    private lateinit var kouluttajavaltuutusRepository: KouluttajavaltuutusRepository

    @Mock
    private lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository

    @Mock
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Mock
    private lateinit var koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository

    @Mock
    private lateinit var koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository

    @Mock
    private lateinit var koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository

    @Mock
    private lateinit var koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository

    @Mock
    private lateinit var koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository

    @Mock
    private lateinit var seurantajaksoRepository: SeurantajaksoRepository

    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var avatarValidator: AvatarValidator

    private lateinit var userService: UserServiceImpl

    private val userId = "user-1"

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
            avatarValidator = avatarValidator
        )

        val user = User(id = userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
    }

    @Test
    fun `sets processed avatar bytes when the uploaded avatar is valid`() {
        val avatarBytes = validPng()
        val avatar = MockMultipartFile("avatar", "avatar.png", "image/png", avatarBytes)
        whenever(avatarValidator.validate(any(), any())).thenReturn(AvatarValidationResult.VALID)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val omatTiedotDTO = OmatTiedotDTO(
            email = "user@example.com",
            avatar = avatar,
            avatarUpdated = true
        )

        val result = userService.updateUserDetails(omatTiedotDTO, userId)

        assertThat(result.avatar).isNotNull
        assertThat(result.avatar).isNotEmpty
        verify(avatarValidator).validate(avatarBytes, "image/png")
    }

    @Test
    fun `throws and does not update avatar when validation rejects the uploaded avatar`() {
        val avatarBytes = "not a real image".toByteArray()
        val avatar = MockMultipartFile("avatar", "avatar.png", "image/png", avatarBytes)
        whenever(avatarValidator.validate(any(), any())).thenReturn(AvatarValidationResult.INVALID_FORMAT)

        val omatTiedotDTO = OmatTiedotDTO(
            email = "user@example.com",
            avatar = avatar,
            avatarUpdated = true
        )

        assertThatThrownBy { userService.updateUserDetails(omatTiedotDTO, userId) }
            .isInstanceOf(BadRequestAlertException::class.java)

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `clears avatar when avatarUpdated is true but no file is provided`() {
        val user = User(id = userId, avatar = "old-avatar".toByteArray())
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val omatTiedotDTO = OmatTiedotDTO(
            email = "user@example.com",
            avatar = null,
            avatarUpdated = true
        )

        val result = userService.updateUserDetails(omatTiedotDTO, userId)

        assertThat(result.avatar).isNull()
        verify(avatarValidator, never()).validate(any(), any())
    }

    @Test
    fun `leaves avatar untouched when avatarUpdated is false`() {
        val existingAvatar = "existing-avatar".toByteArray()
        val user = User(id = userId, avatar = existingAvatar)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val omatTiedotDTO = OmatTiedotDTO(
            email = "user@example.com",
            avatarUpdated = false
        )

        userService.updateUserDetails(omatTiedotDTO, userId)

        assertThat(user.avatar).isEqualTo(existingAvatar)
        verify(avatarValidator, never()).validate(any(), any())
    }

    private fun validPng(): ByteArray {
        val image = java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val output = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "png", output)
        return output.toByteArray()
    }
}
