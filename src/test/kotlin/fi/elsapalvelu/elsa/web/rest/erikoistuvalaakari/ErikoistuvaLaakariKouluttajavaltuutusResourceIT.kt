package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import javax.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariKouluttajavaltuutusResourceIT {

    @Autowired
    private lateinit var kouluttajavaltuutusRepository: KouluttajavaltuutusRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKouluttajavaltuutusMockMvc: MockMvc

    @Autowired
    private lateinit var kayttajaMapper: KayttajaMapper

    private lateinit var kouluttajavaltuutus: Kouluttajavaltuutus

    private lateinit var user: User

    private lateinit var kouluttaja: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKouluttajavaltuutukset() {
        initTest()

        restKouluttajavaltuutusMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/kouluttajavaltuutukset"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    @Transactional
    fun getKouluttajavaltuutuksetExisting() {
        initTest()

        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/kouluttajavaltuutukset"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    @Transactional
    fun getKouluttajavaltuutuksetExpired() {
        initTest()

        kouluttajavaltuutus.paattymispaiva = LocalDate.now().minusDays(1)
        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/kouluttajavaltuutukset"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @Transactional
    fun createKouluttajavaltuutus() {
        initTest()

        val databaseSizeBeforeCreate = kouluttajavaltuutusRepository.findAll().size

        val kayttajaDTO =
            kayttajaMapper.toDto(kouluttaja)

        restKouluttajavaltuutusMockMvc.perform(
            post("/api/erikoistuva-laakari/kouluttajavaltuutus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeCreate + 1)

        val testValtuutus = kouluttajavaltuutusList[kouluttajavaltuutusList.size - 1]
        assertThat(testValtuutus.alkamispaiva).isEqualTo(LocalDate.now())
        assertThat(testValtuutus.paattymispaiva).isEqualTo(LocalDate.now().plusMonths(6))
        assertThat(testValtuutus.valtuuttaja?.kayttaja?.user?.id).isEqualTo(user.id)
        assertThat(testValtuutus.valtuutettu?.id).isEqualTo(kouluttaja.id)
    }

    @Test
    @Transactional
    fun createKouluttajavaltuutusWithExisting() {
        initTest()

        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        val databaseSizeBeforeCreate = kouluttajavaltuutusRepository.findAll().size

        val kayttajaDTO =
            kayttajaMapper.toDto(kouluttaja)

        restKouluttajavaltuutusMockMvc.perform(
            post("/api/erikoistuva-laakari/kouluttajavaltuutus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun updateKouluttajavaltuutus() {
        initTest()

        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        val databaseSizeBeforeCreate = kouluttajavaltuutusRepository.findAll().size

        val kouluttajavaltuutusDTO =
            KouluttajavaltuutusDTO(paattymispaiva = LocalDate.now().plusMonths(2))

        restKouluttajavaltuutusMockMvc.perform(
            put("/api/erikoistuva-laakari/kouluttajavaltuutus/" + kouluttajavaltuutus.id.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeCreate)

        val testValtuutus = kouluttajavaltuutusList[kouluttajavaltuutusList.size - 1]
        assertThat(testValtuutus.alkamispaiva).isEqualTo(kouluttajavaltuutus.alkamispaiva)
        assertThat(testValtuutus.paattymispaiva).isEqualTo(LocalDate.now().plusMonths(2))
        assertThat(testValtuutus.valtuuttaja?.kayttaja?.user?.id).isEqualTo(user.id)
        assertThat(testValtuutus.valtuutettu?.id).isEqualTo(kouluttaja.id)
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

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)

        kouluttaja = KayttajaHelper.createEntity(em)
        em.persist(kouluttaja)

        kouluttajavaltuutus = createKouluttajavaltuutus(erikoistuvaLaakari, kouluttaja)
    }

    companion object {
        @JvmStatic
        fun createKouluttajavaltuutus(
            valtuuttaja: ErikoistuvaLaakari,
            valtuutettu: Kayttaja
        ): Kouluttajavaltuutus {
            return Kouluttajavaltuutus(
                alkamispaiva = LocalDate.now().minusDays(10),
                paattymispaiva = LocalDate.now(),
                valtuutuksenLuontiaika = Instant.now(),
                valtuutuksenMuokkausaika = Instant.now(),
                valtuuttaja = valtuuttaja,
                valtuutettu = valtuutettu
            )
        }
    }
}
