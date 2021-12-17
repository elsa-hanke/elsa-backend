package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import org.hamcrest.Matchers
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager


@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class TekninenPaakayttajaKayttajahallintaResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restMockMvc: MockMvc

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKayttaja() {
        initTest()

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.kayttaja?.id

        restMockMvc.perform(get("/api/tekninen-paakayttaja/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.user.id").exists())
            .andExpect(jsonPath("$.kayttaja.id").value(id))
            .andExpect(jsonPath("$.erikoistuvaLaakari.id").exists())
    }

    @Test
    @Transactional
    fun getNonExistingKayttaja() {
        initTest()

        val id = count.incrementAndGet()

        restMockMvc.perform(get("/api/tekninen-paakayttaja/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.user").isEmpty)
            .andExpect(jsonPath("$.kayttaja").isEmpty)
            .andExpect(jsonPath("$.erikoistuvaLaakari").isEmpty)
    }

    @Test
    @Transactional
    fun getKayttajaForm() {
        initTest()

        val yliopisto = Yliopisto(nimi = ErikoistuvaLaakariHelper.DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        restMockMvc.perform(get("/api/tekninen-paakayttaja/kayttaja-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.yliopistot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(60)))
    }

    @Test
    @Transactional
    fun getErikoistuvatLaakarit() {
        initTest()

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)
        em.flush()

        restMockMvc.perform(get("/api/tekninen-paakayttaja/erikoistuvat-laakarit"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].id").exists())
    }

    @Test
    @Transactional
    fun postErikoistuvaLaakari() {
        initTest()

        val yliopisto = Yliopisto(nimi = ErikoistuvaLaakariHelper.DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val asetus = em.findAll(Asetus::class).get(0)

        val opintoopas = em.findAll(Opintoopas::class).get(0)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id
        kayttajahallintaErikoistuvaLaakariDTO.asetusId = asetus.id
        kayttajahallintaErikoistuvaLaakariDTO.opintoopasId = opintoopas.id

        restMockMvc.perform(
            post("/api/tekninen-paakayttaja/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)
    }

    @Test
    @Transactional
    fun postErikoistuvaLaakariWithExistingEmail() {
        initTest()

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val yliopisto = Yliopisto(nimi = ErikoistuvaLaakariHelper.DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id
        kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite = erikoistuvaLaakari.kayttaja?.user?.email

        restMockMvc.perform(
            post("/api/tekninen-paakayttaja/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun postErikoistuvaLaakariWithoutValidYliopisto() {
        initTest()

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = count.incrementAndGet()
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id

        restMockMvc.perform(
            post("/api/tekninen-paakayttaja/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun postErikoistuvaLaakariWithoutValidErikoisala() {
        initTest()

        val yliopisto = Yliopisto(nimi = ErikoistuvaLaakariHelper.DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        val kayttajahallintaErikoistuvaLaakariDTO = createKayttajahallintaErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = count.incrementAndGet()

        restMockMvc.perform(
            post("/api/tekninen-paakayttaja/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun resendErikoistuvaLaakariInvitation() {
        initTest()

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.id

        val verificationToken = VerificationToken(
            user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)
        )
        em.persist(verificationToken)

        restMockMvc.perform(
            put("/api/tekninen-paakayttaja/erikoistuvat-laakarit/$id/kutsu")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isNoContent)
    }

    fun initTest(createVastuuhenkilonArvio: Boolean? = true) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(TEKNINEN_PAAKAYTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
    }

    companion object {
        private val random: Random = Random()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))

        @JvmStatic
        fun createKayttajahallintaErikoistuvaLaakariDTO(): KayttajahallintaErikoistuvaLaakariDTO {
            return KayttajahallintaErikoistuvaLaakariDTO(
                etunimi = "John",
                sukunimi = "Doe",
                sahkopostiosoite = "john.doe@example.com",
                opiskelijatunnus = "123456",
                opintooikeusAlkaa = LocalDate.ofEpochDay(0L),
                opintooikeusPaattyy = LocalDate.ofEpochDay(30L),
                osaamisenArvioinninOppaanPvm = LocalDate.ofEpochDay(0L)
            )
        }
    }
}
