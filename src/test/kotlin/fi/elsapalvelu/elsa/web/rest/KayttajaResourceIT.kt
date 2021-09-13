package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.persistence.EntityManager


@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajaResourceIT {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKayttajaMockMvc: MockMvc

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun testUserDetailsUpdateWithInvalidEmail() {
        initTest()

        restKayttajaMockMvc.perform(
            multipart("/api/kayttaja")
                .param("email", "system@localhost")
                .param("phoneNumber", UPDATED_PHONE_NUMBER)
                .param("avatarUpdated", "false")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun testUserDetailsUpdateWithAvatar() {
        initTest()

        val imageBaos = ByteArrayOutputStream()
        ImageIO.write(
            BufferedImage(
                256, 256,
                BufferedImage.TYPE_INT_RGB
            ), "jpg", imageBaos
        )

        restKayttajaMockMvc.perform(
            multipart("/api/kayttaja")
                .file(
                    MockMultipartFile(
                        "avatar",
                        "avatar.jpg",
                        "image/jpg",
                        imageBaos.toByteArray()
                    )
                )
                .param("email", UPDATED_EMAIL)
                .param("phoneNumber", UPDATED_PHONE_NUMBER)
                .param("avatarUpdated", "true")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val updatedUser = userRepository.findOneByEmail(UPDATED_EMAIL).get()
        Assertions.assertThat(updatedUser.email).isEqualTo(UPDATED_EMAIL)
        Assertions.assertThat(updatedUser.phoneNumber).isEqualTo(UPDATED_PHONE_NUMBER)
        Assertions.assertThat(updatedUser.avatar).isNotEmpty
    }

    @Test
    @Transactional
    fun testUserDetailsUpdateWithInvalidAvatar() {
        initTest()

        restKayttajaMockMvc.perform(
            multipart("/api/kayttaja")
                .file(
                    MockMultipartFile(
                        "avatar",
                        "avatar.jpg",
                        "image/jpg",
                        UPDATED_AVATAR
                    )
                )
                .param("email", UPDATED_EMAIL)
                .param("phoneNumber", UPDATED_PHONE_NUMBER)
                .param("avatarUpdated", "true")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val updatedUser = userRepository.findOneByEmail(UPDATED_EMAIL).get()
        Assertions.assertThat(updatedUser.email).isEqualTo(UPDATED_EMAIL)
        Assertions.assertThat(updatedUser.phoneNumber).isEqualTo(UPDATED_PHONE_NUMBER)
        Assertions.assertThat(updatedUser.avatar).isNotEmpty
    }

    @Test
    @Transactional
    fun testUserDetailsUpdateWithoutAvatar() {
        initTest()

        restKayttajaMockMvc.perform(
            multipart("/api/kayttaja")
                .param("email", UPDATED_EMAIL)
                .param("phoneNumber", UPDATED_PHONE_NUMBER)
                .param("avatarUpdated", "false")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val updatedUser = userRepository.findOneByEmail(UPDATED_EMAIL).get()
        Assertions.assertThat(updatedUser.email).isEqualTo(UPDATED_EMAIL)
        Assertions.assertThat(updatedUser.phoneNumber).isEqualTo(UPDATED_PHONE_NUMBER)
        Assertions.assertThat(updatedUser.avatar).isEqualTo(DEFAULT_AVATAR)
    }

    @Test
    @Transactional
    fun testUserDetailsUpdateAndAvatarRemoval() {
        initTest()

        restKayttajaMockMvc.perform(
            multipart("/api/kayttaja")
                .param("email", UPDATED_EMAIL)
                .param("phoneNumber", UPDATED_PHONE_NUMBER)
                .param("avatar", null)
                .param("avatarUpdated", "true")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val updatedUser = userRepository.findOneByEmail(UPDATED_EMAIL).get()
        Assertions.assertThat(updatedUser.email).isEqualTo(UPDATED_EMAIL)
        Assertions.assertThat(updatedUser.phoneNumber).isEqualTo(UPDATED_PHONE_NUMBER)
        Assertions.assertThat(updatedUser.avatar).isEqualTo(null)
    }

    fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        user.avatar = DEFAULT_AVATAR
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
    }


    companion object {
        private const val UPDATED_EMAIL = "test@localhost"
        private const val UPDATED_PHONE_NUMBER = "0000000000"
        private val DEFAULT_AVATAR: ByteArray = createByteArray(1, "0")
        private val UPDATED_AVATAR: ByteArray = createByteArray(1, "1")
    }
}
