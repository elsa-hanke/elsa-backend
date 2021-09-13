package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.EpaOsaamisalueHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariSuoritusarviointiResourceIT {

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var suoritusarviointiMapper: SuoritusarviointiMapper

    @Autowired
    private lateinit var suoritusarviointiService: SuoritusarviointiService

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restSuoritusarviointiMockMvc: MockMvc

    private lateinit var suoritusarviointi: Suoritusarviointi

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Lisätään voimassaoleva EPA-osaamisalue ja päättymistä ei määritetty
        em.persist(EpaOsaamisalueHelper.createEntity(em, LocalDate.ofEpochDay(0L), null))
        // Lisätään voimassaoleva EPA-osaamisalue ja päättyminen määritetty
        em.persist(EpaOsaamisalueHelper.createEntity(em, LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(20L)))
        // Lisätään EPA-osaamisalue jonka voimassaolo ei ole alkanut vielä
        em.persist(EpaOsaamisalueHelper.createEntity(em, LocalDate.ofEpochDay(15L), LocalDate.ofEpochDay(20L)))
        // Lisätään EPA-osaamisalue, jonka voimassaolo on jo päättynyt
        em.persist(EpaOsaamisalueHelper.createEntity(em, LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(5L)))

        // Lisätään voimassaoleva erikoisala ja päättymistä ei määritetty
        em.persist(ErikoisalaHelper.createEntity(LocalDate.ofEpochDay(0L), null))
        // Lisätään voimassaoleva erikoisala ja päättyminen määritetty
        em.persist(ErikoisalaHelper.createEntity(LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(20L)))
        // Lisätään erikoisala jonka voimassaolo ei ole alkanut vielä
        em.persist(ErikoisalaHelper.createEntity(LocalDate.ofEpochDay(15L), LocalDate.ofEpochDay(20L)))
        // Lisätään erikoisala, jonka voimassaolo on jo päättynyt
        em.persist(ErikoisalaHelper.createEntity(LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(5L)))

        em.flush()
    }

    @Test
    @Transactional
    fun createSuoritusarviointi() {
        initTest()

        val databaseSizeBeforeCreate = suoritusarviointiRepository.findAll().size

        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(suoritusarviointi)
        restSuoritusarviointiMockMvc.perform(
            post("/api/erikoistuva-laakari/suoritusarvioinnit/arviointipyynto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeCreate + 1)
        val testSuoritusarviointi = suoritusarviointiList[suoritusarviointiList.size - 1]
        assertThat(testSuoritusarviointi.tapahtumanAjankohta).isEqualTo(DEFAULT_TAPAHTUMAN_AJANKOHTA)
        assertThat(testSuoritusarviointi.arvioitavaTapahtuma).isEqualTo(DEFAULT_ARVIOITAVA_TAPAHTUMA)
        assertThat(testSuoritusarviointi.lisatiedot).isEqualTo(DEFAULT_LISATIEDOT)
    }

    @Test
    @Transactional
    fun updateSuoritusarviointi() {
        initTest()

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val databaseSizeBeforeUpdate = suoritusarviointiRepository.findAll().size

        val id = suoritusarviointi.id
        assertNotNull(id)
        val updatedSuoritusarviointi = suoritusarviointiRepository.findById(id).get()
        em.detach(updatedSuoritusarviointi)
        updatedSuoritusarviointi.tapahtumanAjankohta = UPDATED_TAPAHTUMAN_AJANKOHTA
        updatedSuoritusarviointi.arvioitavaTapahtuma = UPDATED_ARVIOITAVA_TAPAHTUMA
        updatedSuoritusarviointi.pyynnonAika = UPDATED_PYYNNON_AIKA
        updatedSuoritusarviointi.lisatiedot = UPDATED_LISATIEDOT
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(updatedSuoritusarviointi)

        restSuoritusarviointiMockMvc.perform(
            put("/api/erikoistuva-laakari/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testSuoritusarviointi = suoritusarviointiList[suoritusarviointiList.size - 1]
        assertThat(testSuoritusarviointi.tapahtumanAjankohta).isEqualTo(UPDATED_TAPAHTUMAN_AJANKOHTA)
        assertThat(testSuoritusarviointi.arvioitavaTapahtuma).isEqualTo(UPDATED_ARVIOITAVA_TAPAHTUMA)
        assertThat(testSuoritusarviointi.lisatiedot).isEqualTo(UPDATED_LISATIEDOT)
    }

    @Test
    @Transactional
    fun getAllSuoritusarvioinnit() {
        initTest()

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        restSuoritusarviointiMockMvc.perform(get("/api/erikoistuva-laakari/suoritusarvioinnit"))
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
                "/api/erikoistuva-laakari/suoritusarvioinnit/{id}",
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

    @Test
    @Transactional
    fun deleteSuoritusarviointi() {
        initTest()

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val databaseSizeBeforeDelete = suoritusarviointiRepository.findAll().size

        restSuoritusarviointiMockMvc.perform(
            delete("/api/erikoistuva-laakari/suoritusarvioinnit/{id}", suoritusarviointi.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun getSuoritusarvioinnitRajaimet() {
        initTest()

        restSuoritusarviointiMockMvc.perform(get("/api/erikoistuva-laakari/suoritusarvioinnit-rajaimet"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.epaOsaamisalueet").value(Matchers.hasSize<Any>(2))) // 2 voimassaolevaa
            .andExpect(jsonPath("$.tapahtumat").value(Matchers.hasSize<Any>(0)))
            .andExpect(jsonPath("$.kouluttajatAndVastuuhenkilot").value(Matchers.hasSize<Any>(0)))
    }

    @Test
    @Transactional
    fun getArviointipyyntoForm() {
        initTest()

        restSuoritusarviointiMockMvc.perform(get("/api/erikoistuva-laakari/arviointipyynto-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.kunnat").value(Matchers.hasSize<Any>(478)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(2))) // 2 voimassaolevaa
            .andExpect(jsonPath("$.epaOsaamisalueenKategoriat").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.epaOsaamisalueenKategoriat.[0].epaOsaamisalueet").value(Matchers.hasSize<Any>(2))) // 2 voimassaolevaa
            .andExpect(jsonPath("$.kouluttajatAndVastuuhenkilot").value(Matchers.hasSize<Any>(0)))
    }

    fun initTest(userId: String? = DEFAULT_ID) {
        user = KayttajaResourceWithMockUserIT.createEntity()
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
                kayttaja = KayttajaHelper.createEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Lisätään pakollinen tieto
            val epaOsaamisalue: EpaOsaamisalue
            if (em.findAll(EpaOsaamisalue::class).isEmpty()) {
                epaOsaamisalue = EpaOsaamisalueHelper.createEntity(em)
                em.persist(epaOsaamisalue)
                em.flush()
            } else {
                epaOsaamisalue = em.findAll(EpaOsaamisalue::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = epaOsaamisalue

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, user)
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
            val epaOsaamisalue: EpaOsaamisalue
            if (em.findAll(EpaOsaamisalue::class).isEmpty()) {
                epaOsaamisalue = EpaOsaamisalueHelper.createUpdatedEntity(em)
                em.persist(epaOsaamisalue)
                em.flush()
            } else {
                epaOsaamisalue = em.findAll(EpaOsaamisalue::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = epaOsaamisalue

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
