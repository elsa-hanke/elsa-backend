package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import junit.framework.TestCase.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.hibernate.engine.jdbc.BlobProxy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
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
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariAsiakirjaResourceIT {

    @Autowired
    private lateinit var asiakirjaRepository: AsiakirjaRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restAsiakirjaMockMvc: MockMvc

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var opintooikeusRepository: OpintooikeusRepository

    private lateinit var asiakirja: Asiakirja

    private lateinit var tempFile1: File

    private lateinit var tempFile2: File

    private lateinit var mockMultipartFile1: MockMultipartFile

    private lateinit var mockMultipartFile2: MockMultipartFile

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
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
        assertThat(testAsiakirja.asiakirjaData?.data?.binaryStream?.readBytes()).isEqualTo(
            AsiakirjaHelper.ASIAKIRJA_PDF_DATA
        )
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
        assertThat(testAsiakirja.asiakirjaData?.data?.binaryStream?.readBytes()).isEqualTo(
            AsiakirjaHelper.ASIAKIRJA_PDF_DATA
        )
        assertThat(testAsiakirja.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())

        val testAsiakirja2 = asiakirjaList[asiakirjaList.size - 1]
        assertThat(testAsiakirja2.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI)
        assertThat(testAsiakirja2.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI)
        assertThat(testAsiakirja2.asiakirjaData?.data?.binaryStream?.readBytes()).isEqualTo(
            AsiakirjaHelper.ASIAKIRJA_PNG_DATA
        )
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
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)
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

        val asiakirja2 = AsiakirjaHelper.createEntity(em, user)
        asiakirja2.nimi = AsiakirjaHelper.ASIAKIRJA_PNG_NIMI
        asiakirja2.tyyppi = AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI
        asiakirja2.lisattypvm = LocalDateTime.now().minusDays(1)
        asiakirja2.asiakirjaData?.data = BlobProxy.generateProxy(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)

        asiakirjaRepository.saveAndFlush(asiakirja2)

        restAsiakirjaMockMvc.perform(get("/api/erikoistuva-laakari/asiakirjat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].nimi").value(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI))
            .andExpect(
                jsonPath("$[0].lisattypvm").value(
                    Matchers.containsString(
                        LocalDate.now().toString()
                    )
                )
            )
            .andExpect(jsonPath("$[0].tyyppi").value(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI))
            .andExpect(jsonPath("$[1].asiakirjaData.fileInputStream").doesNotExist())
            .andExpect(jsonPath("$[1].asiakirjaData.fileSize").doesNotExist())
            .andExpect(jsonPath("$[1].id").exists())
            .andExpect(jsonPath("$[1].nimi").value(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI))
            .andExpect(
                jsonPath("$[1].lisattypvm").value(
                    Matchers.containsString(
                        LocalDate.now().minusDays(1).toString()
                    )
                )
            )
            .andExpect(jsonPath("$[1].tyyppi").value(AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI))
            .andExpect(jsonPath("$[1].asiakirjaData.fileInputStream").doesNotExist())
            .andExpect(jsonPath("$[1].asiakirjaData.fileSize").doesNotExist())
    }

    @Test
    @Transactional
    fun getAsiakirjatShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()
        asiakirjaRepository.saveAndFlush(asiakirja)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        val newOpintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        val asiakirjaForAnotherOpintooikeus = AsiakirjaHelper.createEntity(em, user)
        em.persist(asiakirjaForAnotherOpintooikeus)

        restAsiakirjaMockMvc.perform(get("/api/erikoistuva-laakari/asiakirjat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].id").value(asiakirjaForAnotherOpintooikeus.id))
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
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)
        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

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

    fun initTest(userId: String? = null) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        asiakirja = AsiakirjaHelper.createEntity(em, user)
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
