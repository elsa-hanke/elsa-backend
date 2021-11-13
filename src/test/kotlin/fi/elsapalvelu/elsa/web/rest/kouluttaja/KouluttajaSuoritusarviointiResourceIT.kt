package fi.elsapalvelu.elsa.web.rest.kouluttaja

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.ArvioinninPerustuminen
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ArvioitavaKokonaisuusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KouluttajaSuoritusarviointiResourceIT {

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var suoritusarviointiMapper: SuoritusarviointiMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restSuoritusarviointiMockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var suoritusarviointi: Suoritusarviointi

    private lateinit var user: User

    private lateinit var tempFile: File

    private lateinit var mockMultipartFile: MockMultipartFile

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun updateSuoritusarviointi() {
        initTest()
        initMockFile()

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val databaseSizeBeforeUpdate = suoritusarviointiRepository.findAll().size

        val arviointityokalut = mutableSetOf(em.findAll(Arviointityokalu::class).get(0))

        val id = suoritusarviointi.id
        assertNotNull(id)
        val updatedSuoritusarviointi = suoritusarviointiRepository.findById(id).get()
        em.detach(updatedSuoritusarviointi)
        updatedSuoritusarviointi.arviointiAika = UPDATED_TAPAHTUMAN_AJANKOHTA
        updatedSuoritusarviointi.arviointiPerustuu = ArvioinninPerustuminen.LASNA
        updatedSuoritusarviointi.arviointityokalut = arviointityokalut
        updatedSuoritusarviointi.sanallinenArviointi = UPDATED_LISATIEDOT
        updatedSuoritusarviointi.arviointiasteikonTaso = 5
        updatedSuoritusarviointi.vaativuustaso = 5
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(updatedSuoritusarviointi).apply {
            arviointiAsiakirjaUpdated = true
        }

        val updatedSuoritusarviointiJson = objectMapper.writeValueAsString(suoritusarviointiDTO)

        restSuoritusarviointiMockMvc.perform(
            multipart("/api/kouluttaja/suoritusarvioinnit")
                .file(mockMultipartFile)
                .param("suoritusarviointiJson", updatedSuoritusarviointiJson)
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testSuoritusarviointi = suoritusarviointiList[suoritusarviointiList.size - 1]
        assertThat(testSuoritusarviointi.arviointiPerustuu).isEqualTo(ArvioinninPerustuminen.LASNA)
        assertThat(testSuoritusarviointi.arviointiAika).isEqualTo(UPDATED_TAPAHTUMAN_AJANKOHTA)
        assertThat(testSuoritusarviointi.arviointityokalut).isEqualTo(arviointityokalut)
        assertThat(testSuoritusarviointi.sanallinenArviointi).isEqualTo(UPDATED_LISATIEDOT)
        assertThat(testSuoritusarviointi.arviointiasteikonTaso).isEqualTo(5)
        assertThat(testSuoritusarviointi.vaativuustaso).isEqualTo(5)
        assertThat(testSuoritusarviointi.arviointiLiiteNimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(testSuoritusarviointi.arviointiLiiteTyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI)
        val asiakirjaData = testSuoritusarviointi.asiakirjaData
        assertThat(asiakirjaData?.data?.binaryStream?.readBytes()).isEqualTo(
            AsiakirjaHelper.ASIAKIRJA_PDF_DATA
        )
    }

    fun initMockFile() {
        tempFile = File.createTempFile("file", "pdf")
        tempFile.writeBytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        tempFile.deleteOnExit()

        mockMultipartFile = MockMultipartFile(
            "arviointiFile",
            AsiakirjaHelper.ASIAKIRJA_PDF_NIMI,
            AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI,
            tempFile.readBytes()
        )
    }

    @Test
    @Transactional
    fun getAllSuoritusarvioinnit() {
        initTest()

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        restSuoritusarviointiMockMvc.perform(get("/api/kouluttaja/suoritusarvioinnit"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
    }

    @Test
    @Transactional
    fun getSuoritusarviointi() {
        initTest()

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val id = suoritusarviointi.id
        assertNotNull(id)

        restSuoritusarviointiMockMvc.perform(
            get(
                "/api/kouluttaja/suoritusarvioinnit/{id}",
                id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(suoritusarviointi.id!!))
            .andExpect(jsonPath("$.tapahtumanAjankohta").value(DEFAULT_TAPAHTUMAN_AJANKOHTA.toString()))
            .andExpect(jsonPath("$.arvioitavaTapahtuma").value(DEFAULT_ARVIOITAVA_TAPAHTUMA))
            .andExpect(jsonPath("$.pyynnonAika").value(DEFAULT_PYYNNON_AIKA.toString()))
            .andExpect(jsonPath("$.lisatiedot").value(DEFAULT_LISATIEDOT))
    }

    fun initTest(userId: String? = DEFAULT_ID) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(KOULUTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        suoritusarviointi = createEntity(em, user)
    }

    companion object {

        private const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        private const val DEFAULT_LOGIN = "johndoe"
        private const val DEFAULT_EMAIL = "john.doe@example.com"

        private val DEFAULT_TAPAHTUMAN_AJANKOHTA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_TAPAHTUMAN_AJANKOHTA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_ARVIOITAVA_TAPAHTUMA = "AAAAAAAAAA"
        private const val UPDATED_ARVIOITAVA_TAPAHTUMA = "BBBBBBBBBB"

        private val DEFAULT_PYYNNON_AIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PYYNNON_AIKA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LISATIEDOT = "AAAAAAAAAA"
        private const val UPDATED_LISATIEDOT = "BBBBBBBBBB"

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = DEFAULT_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = DEFAULT_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = DEFAULT_PYYNNON_AIKA,
                lisatiedot = DEFAULT_LISATIEDOT
            )

            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createEntity(em, user)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Lisätään pakollinen tieto
            val arvioitavaKokonaisuus: ArvioitavaKokonaisuus
            if (em.findAll(ArvioitavaKokonaisuus::class).isEmpty()) {
                arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
                em.persist(arvioitavaKokonaisuus)
                em.flush()
            } else {
                arvioitavaKokonaisuus = em.findAll(ArvioitavaKokonaisuus::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = arvioitavaKokonaisuus

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso

            return suoritusarviointi
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = UPDATED_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = UPDATED_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = UPDATED_PYYNNON_AIKA,
                lisatiedot = UPDATED_LISATIEDOT
            )

            // Lisätään pakollinen tieto
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaHelper.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Lisätään pakollinen tieto
            val arvioitavaKokonaisuus: ArvioitavaKokonaisuus
            if (em.findAll(ArvioitavaKokonaisuus::class).isEmpty()) {
                arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createUpdatedEntity(em)
                em.persist(arvioitavaKokonaisuus)
                em.flush()
            } else {
                arvioitavaKokonaisuus = em.findAll(ArvioitavaKokonaisuus::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = arvioitavaKokonaisuus

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createUpdatedEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso

            return suoritusarviointi
        }
    }
}
