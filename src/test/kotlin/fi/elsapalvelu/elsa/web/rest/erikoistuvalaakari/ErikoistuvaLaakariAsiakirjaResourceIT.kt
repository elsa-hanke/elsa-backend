package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.repository.AsiakirjaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import junit.framework.TestCase.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class ErikoistuvaLaakariAsiakirjaResourceIT {

    @Autowired
    private lateinit var asiakirjaRepository: AsiakirjaRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restAsiakirjaMockMvc: MockMvc

    private lateinit var asiakirja: Asiakirja

    private lateinit var tempFile1: File

    private lateinit var tempFile2: File

    private lateinit var mockMultipartFile1: MockMultipartFile

    private lateinit var mockMultipartFile2: MockMultipartFile

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun createAsiakirja_SingleFile() {
        initTest()
        initMockFiles()

        val databaseSizeBeforeCreate = asiakirjaRepository.findAll().size

        restAsiakirjaMockMvc.perform(
            multipart("/api/erikoistuva-laakari/asiakirjat")
                .file(mockMultipartFile1)
                .with(csrf())
        ).andExpect(status().isCreated)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeCreate + 1)

        val testAsiakirja = asiakirjaList[asiakirjaList.size - 1]
        assertThat(testAsiakirja.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(testAsiakirja.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI)
        assertThat(testAsiakirja.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        assertThat(testAsiakirja.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
    }

    @Test
    @Transactional
    fun createAsiakirja_MultipleFiles() {
        initTest()
        initMockFiles()

        val databaseSizeBeforeCreate = asiakirjaRepository.findAll().size

        restAsiakirjaMockMvc.perform(
            multipart("/api/erikoistuva-laakari/asiakirjat")
                .file(mockMultipartFile1).file(mockMultipartFile2)
                .with(csrf())
        ).andExpect(status().isCreated)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeCreate + 2)

        val testAsiakirja = asiakirjaList[asiakirjaList.size - 2]
        assertThat(testAsiakirja.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(testAsiakirja.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI)
        assertThat(testAsiakirja.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        assertThat(testAsiakirja.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())

        val testAsiakirja2 = asiakirjaList[asiakirjaList.size - 1]
        assertThat(testAsiakirja2.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI)
        assertThat(testAsiakirja2.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI)
        assertThat(testAsiakirja2.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
        assertThat(testAsiakirja2.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
    }

    @Test
    @Transactional
    fun createAsiakirjaWithExistingFileName() {
        initTest()
        initMockFiles()

        asiakirjaRepository.saveAndFlush(asiakirja)

        val databaseSizeBeforeCreate = asiakirjaRepository.findAll().size

        restAsiakirjaMockMvc.perform(
            multipart("/api/erikoistuva-laakari/asiakirjat")
                .file(mockMultipartFile1)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createAsiakirja_MultipleFiles_AnotherWithExistingFileName() {
        initTest()
        initMockFiles()

        asiakirjaRepository.saveAndFlush(asiakirja)

        val databaseSizeBeforeCreate = asiakirjaRepository.findAll().size

        restAsiakirjaMockMvc.perform(
            multipart("/api/erikoistuva-laakari/asiakirjat")
                .file(mockMultipartFile1).file(mockMultipartFile2)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createAsiakirjaWithInvalidContentType() {
        initTest()
        initMockFiles()

        val databaseSizeBeforeCreate = asiakirjaRepository.findAll().size

        mockMultipartFile1 = MockMultipartFile(
            "files",
            "asiakirja.doc",
            "application/msword",
            tempFile1.readBytes()
        )

        restAsiakirjaMockMvc.perform(
            multipart("/api/erikoistuva-laakari/asiakirjat")
                .file(mockMultipartFile1)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun userShouldNotSeeAsiakirjaForAnotherUser() {
        initTest(null)
        asiakirjaRepository.saveAndFlush(asiakirja)

        restAsiakirjaMockMvc.perform(get("/api/erikoistuva-laakari/asiakirjat"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(0)))
    }

    @Test
    @Transactional
    fun getAsiakirjat() {
        initTest()

        asiakirjaRepository.saveAndFlush(asiakirja)
        em.detach(asiakirja)

        val asiakirja2 = AsiakirjaHelper.createEntity(em, KayttajaHelper.DEFAULT_ID)
        asiakirja2.nimi = AsiakirjaHelper.ASIAKIRJA_PNG_NIMI
        asiakirja2.tyyppi = AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI
        asiakirja2.lisattypvm = LocalDateTime.now().minusDays(1)
        asiakirja2.data = AsiakirjaHelper.ASIAKIRJA_PNG_DATA

        asiakirjaRepository.saveAndFlush(asiakirja2)
        em.detach(asiakirja2)

        restAsiakirjaMockMvc.perform(get("/api/erikoistuva-laakari/asiakirjat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].nimi").value(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI))
            .andExpect(jsonPath("$[0].lisattypvm").value(Matchers.containsString(LocalDate.now().toString())))
            .andExpect(jsonPath("$[0].tyyppi").doesNotExist())
            .andExpect(jsonPath("$[0].data").doesNotExist())
            .andExpect(jsonPath("$[1].id").exists())
            .andExpect(jsonPath("$[1].nimi").value(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI))
            .andExpect(
                jsonPath("$[1].lisattypvm").value(
                    Matchers.containsString(
                        LocalDate.now().minusDays(1).toString()
                    )
                )
            )
            .andExpect(jsonPath("$[1].tyyppi").doesNotExist())
            .andExpect(jsonPath("$[1].data").doesNotExist())
    }

    @Test
    @Transactional
    fun getAsiakirja() {
        initTest()

        asiakirjaRepository.saveAndFlush(asiakirja)

        val id = asiakirja.id
        assertNotNull(id)

        restAsiakirjaMockMvc.perform(get("/api/erikoistuva-laakari/asiakirjat/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/pdf;charset=UTF-8"))
            .andExpect(content().bytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA))
    }

    @Test
    @Transactional
    fun deleteAsiakirja() {
        initTest()

        asiakirjaRepository.saveAndFlush(asiakirja)

        val databaseSizeBeforeDelete = asiakirjaRepository.findAll().size

        restAsiakirjaMockMvc.perform(
            delete("/api/erikoistuva-laakari/asiakirjat/{id}", asiakirja.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteAnotherUserAsiakirja() {
        initTest(null)

        asiakirjaRepository.saveAndFlush(asiakirja)

        val databaseSizeBeforeDelete = asiakirjaRepository.findAll().size

        restAsiakirjaMockMvc.perform(
            delete("/api/erikoistuva-laakari/asiakirjat/{id}", asiakirja.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val asiakirjaList = asiakirjaRepository.findAll()
        assertThat(asiakirjaList).hasSize(databaseSizeBeforeDelete)
    }

    fun initTest(userId: String? = KayttajaHelper.DEFAULT_ID) {
        val userDetails = mapOf<String, Any>(
            "uid" to KayttajaHelper.DEFAULT_ID,
            "sub" to KayttajaHelper.DEFAULT_LOGIN,
            "email" to KayttajaHelper.DEFAULT_EMAIL
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val user = DefaultOAuth2User(authorities, userDetails, "sub")
        val authentication = OAuth2AuthenticationToken(user, authorities, "oidc")
        TestSecurityContextHolder.getContext().authentication = authentication

        asiakirja = AsiakirjaHelper.createEntity(em, userId)
    }

    fun initMockFiles() {
        tempFile1 = File.createTempFile("file", "pdf")
        tempFile1.writeBytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        tempFile1.deleteOnExit()

        tempFile2 = File.createTempFile("file", "png")
        tempFile2.writeBytes(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
        tempFile2.deleteOnExit()

        mockMultipartFile1 = MockMultipartFile(
            "files",
            AsiakirjaHelper.ASIAKIRJA_PDF_NIMI,
            AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI,
            tempFile1.readBytes()
        )

        mockMultipartFile2 = MockMultipartFile(
            "files",
            AsiakirjaHelper.ASIAKIRJA_PNG_NIMI,
            AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI,
            tempFile2.readBytes()
        )
    }
}
