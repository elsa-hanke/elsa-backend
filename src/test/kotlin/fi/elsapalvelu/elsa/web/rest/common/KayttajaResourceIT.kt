package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.web.rest.createByteArray
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.imageio.ImageIO
import javax.persistence.EntityManager


@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajaResourceIT {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var yliopistoRepository: YliopistoRepository

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

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

    @Test
    @Transactional
    fun testGetExistingAccount() {
        initTest()

        restKayttajaMockMvc.perform(
            MockMvcRequestBuilders.get("/api/kayttaja")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("\$.authorities").value(ERIKOISTUVA_LAAKARI))
    }

    @Test
    @Transactional
    fun testGetImpersonatedUser() {
        initTest(VASTUUHENKILO)

        val vastuuhenkilo = KayttajaHelper.createEntity(em, user)
        kayttajaRepository.save(vastuuhenkilo)

        val yliopisto1 = yliopistoRepository.save(Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO))

        val erikoisala1 = erikoisalaRepository.findById(1).get()

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto1,
            erikoisala = erikoisala1
        )
        kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(5)
            )
        erikoistuvaLaakariRepository.save(erikoistuvaLaakari)

        restKayttajaMockMvc.perform(
            MockMvcRequestBuilders.get("/api/login/impersonate?erikoistuvaLaakariId=${erikoistuvaLaakari.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isFound)

        // TestSecurityContext täytyy päivittää käsin, koska impersonointi päivittää vain security contextin
        val currentAuthentication: Authentication =
            TestSecurityContextHolder.getContext().authentication
        val switchAuthority: GrantedAuthority = SwitchUserGrantedAuthority(
            ERIKOISTUVA_LAAKARI_IMPERSONATED, currentAuthentication
        )
        val currentPrincipal = currentAuthentication.principal as Saml2AuthenticatedPrincipal
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(
            erikoistuvaLaakari.kayttaja?.user?.id,
            mapOf(
                "urn:oid:2.5.4.42" to listOf(erikoistuvaLaakari.kayttaja?.user?.firstName),
                "urn:oid:2.5.4.4" to listOf(erikoistuvaLaakari.kayttaja?.user?.lastName),
                "nameID" to currentPrincipal.attributes["nameID"],
                "nameIDFormat" to currentPrincipal.attributes["nameIDFormat"],
                "nameIDQualifier" to currentPrincipal.attributes["nameIDQualifier"],
                "nameIDSPQualifier" to currentPrincipal.attributes["nameIDSPQualifier"]
            )
        )
        val context = TestSecurityContextHolder.getContext()
        context.authentication = Saml2Authentication(
            newPrincipal,
            (currentAuthentication as Saml2Authentication).saml2Response,
            listOf(switchAuthority)
        )
        TestSecurityContextHolder.setContext(context)

        restKayttajaMockMvc.perform(
            MockMvcRequestBuilders.get("/api/kayttaja")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("\$.authorities").value(ERIKOISTUVA_LAAKARI))
            .andExpect(MockMvcResultMatchers.jsonPath("\$.impersonated").value(true))

        restKayttajaMockMvc.perform(
            MockMvcRequestBuilders.get("/api/kayttaja-impersonated")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("\$.authorities").value(VASTUUHENKILO))
    }

    fun initTest(authority: String = ERIKOISTUVA_LAAKARI) {
        user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(authority))
        user.avatar = DEFAULT_AVATAR
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(authority))
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
