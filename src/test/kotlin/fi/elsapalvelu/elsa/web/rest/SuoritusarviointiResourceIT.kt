package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Validator

/**
 * Integration tests for the [SuoritusarviointiResource] REST controller.
 *
 * @see SuoritusarviointiResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class SuoritusarviointiResourceIT {

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var suoritusarviointiMapper: SuoritusarviointiMapper

    @Autowired
    private lateinit var suoritusarviointiService: SuoritusarviointiService

    @Autowired
    private lateinit var suoritusarviointiQueryService: SuoritusarviointiQueryService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var kayttajaService: KayttajaService

    @Autowired
    private lateinit var tyoskentelyjaksoService: TyoskentelyjaksoService

    @Autowired
    private lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

    @Autowired
    private lateinit var pageableArgumentResolver: PageableHandlerMethodArgumentResolver

    @Autowired
    private lateinit var exceptionTranslator: ExceptionTranslator

    @Autowired
    private lateinit var validator: Validator

    @Autowired
    private lateinit var em: EntityManager

    private lateinit var restSuoritusarviointiMockMvc: MockMvc

    private lateinit var suoritusarviointi: Suoritusarviointi

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val suoritusarviointiResource = SuoritusarviointiResource(
            suoritusarviointiService,
            suoritusarviointiQueryService
        )
         this.restSuoritusarviointiMockMvc = MockMvcBuilders.standaloneSetup(suoritusarviointiResource)
             .setCustomArgumentResolvers(pageableArgumentResolver)
             .setControllerAdvice(exceptionTranslator)
             .setConversionService(createFormattingConversionService())
             .setMessageConverters(jacksonMessageConverter)
             .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        suoritusarviointi = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createSuoritusarviointi() {
        val databaseSizeBeforeCreate = suoritusarviointiRepository.findAll().size

        // Create the Suoritusarviointi
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(suoritusarviointi)
        restSuoritusarviointiMockMvc.perform(
            post("/api/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
        ).andExpect(status().isCreated)

        // Validate the Suoritusarviointi in the database
        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeCreate + 1)
        val testSuoritusarviointi = suoritusarviointiList[suoritusarviointiList.size - 1]
        assertThat(testSuoritusarviointi.tapahtumanAjankohta).isEqualTo(DEFAULT_TAPAHTUMAN_AJANKOHTA)
        assertThat(testSuoritusarviointi.arvioitavaTapahtuma).isEqualTo(DEFAULT_ARVIOITAVA_TAPAHTUMA)
        assertThat(testSuoritusarviointi.pyynnonAika).isEqualTo(DEFAULT_PYYNNON_AIKA)
        assertThat(testSuoritusarviointi.lisatiedot).isEqualTo(DEFAULT_LISATIEDOT)
        assertThat(testSuoritusarviointi.itsearviointiVaativuustaso).isEqualTo(DEFAULT_ITSEARVIOINTI_VAATIVUUSTASO)
        assertThat(testSuoritusarviointi.itsearviointiLuottamuksenTaso).isEqualTo(DEFAULT_ITSEARVIOINTI_LUOTTAMUKSEN_TASO)
        assertThat(testSuoritusarviointi.sanallinenItsearviointi).isEqualTo(DEFAULT_SANALLINEN_ITSEARVIOINTI)
        assertThat(testSuoritusarviointi.vaativuustaso).isEqualTo(DEFAULT_VAATIVUUSTASO)
        assertThat(testSuoritusarviointi.luottamuksenTaso).isEqualTo(DEFAULT_LUOTTAMUKSEN_TASO)
        assertThat(testSuoritusarviointi.sanallinenArviointi).isEqualTo(DEFAULT_SANALLINEN_ARVIOINTI)
        assertThat(testSuoritusarviointi.arviointiAika).isEqualTo(DEFAULT_ARVIOINTI_AIKA)
        assertThat(testSuoritusarviointi.lukittu).isEqualTo(DEFAULT_LUKITTU)
    }

    @Test
    @Transactional
    fun createSuoritusarviointiWithExistingId() {
        val databaseSizeBeforeCreate = suoritusarviointiRepository.findAll().size

        // Create the Suoritusarviointi with an existing ID
        suoritusarviointi.id = 1L
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(suoritusarviointi)

        // An entity with an existing ID cannot be created, so this API call must fail
        restSuoritusarviointiMockMvc.perform(
            post("/api/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Suoritusarviointi in the database
        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkTapahtumanAjankohtaIsRequired() {
        val databaseSizeBeforeTest = suoritusarviointiRepository.findAll().size
        // set the field null
        suoritusarviointi.tapahtumanAjankohta = null

        // Create the Suoritusarviointi, which fails.
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(suoritusarviointi)

        restSuoritusarviointiMockMvc.perform(
            post("/api/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
        ).andExpect(status().isBadRequest)

        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkArvioitavaTapahtumaIsRequired() {
        val databaseSizeBeforeTest = suoritusarviointiRepository.findAll().size
        // set the field null
        suoritusarviointi.arvioitavaTapahtuma = null

        // Create the Suoritusarviointi, which fails.
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(suoritusarviointi)

        restSuoritusarviointiMockMvc.perform(
            post("/api/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
        ).andExpect(status().isBadRequest)

        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllSuoritusarvioinnit() {
        // Initialize the database
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        // Get all the suoritusarviointiList
        restSuoritusarviointiMockMvc.perform(get("/api/suoritusarvioinnit?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(suoritusarviointi.id?.toInt())))
            .andExpect(jsonPath("$.[*].tapahtumanAjankohta").value(hasItem(DEFAULT_TAPAHTUMAN_AJANKOHTA.toString())))
            .andExpect(jsonPath("$.[*].arvioitavaTapahtuma").value(hasItem(DEFAULT_ARVIOITAVA_TAPAHTUMA)))
            .andExpect(jsonPath("$.[*].pyynnonAika").value(hasItem(DEFAULT_PYYNNON_AIKA.toString())))
            .andExpect(jsonPath("$.[*].lisatiedot").value(hasItem(DEFAULT_LISATIEDOT.toString())))
            .andExpect(jsonPath("$.[*].vaativuustaso").value(hasItem(DEFAULT_VAATIVUUSTASO)))
            .andExpect(jsonPath("$.[*].sanallinenArviointi").value(hasItem(DEFAULT_SANALLINEN_ARVIOINTI)))
            .andExpect(jsonPath("$.[*].arviointiAika").value(hasItem(DEFAULT_ARVIOINTI_AIKA.toString()))) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getSuoritusarviointi() {
        // Initialize the database
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val id = suoritusarviointi.id
        assertNotNull(id)

        // Get the suoritusarviointi
        restSuoritusarviointiMockMvc.perform(get("/api/suoritusarvioinnit/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(suoritusarviointi.id as Any))
            .andExpect(jsonPath("$.tapahtumanAjankohta").value(DEFAULT_TAPAHTUMAN_AJANKOHTA.toString()))
            .andExpect(jsonPath("$.arvioitavaTapahtuma").value(DEFAULT_ARVIOITAVA_TAPAHTUMA))
            .andExpect(jsonPath("$.pyynnonAika").value(DEFAULT_PYYNNON_AIKA.toString()))
            .andExpect(jsonPath("$.lisatiedot").value(DEFAULT_LISATIEDOT.toString()))
            .andExpect(jsonPath("$.vaativuustaso").value(DEFAULT_VAATIVUUSTASO))
            .andExpect(jsonPath("$.sanallinenArviointi").value(DEFAULT_SANALLINEN_ARVIOINTI))
            .andExpect(jsonPath("$.arviointiAika").value(DEFAULT_ARVIOINTI_AIKA.toString())) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingSuoritusarviointi() {
        // Get the suoritusarviointi
        restSuoritusarviointiMockMvc.perform(get("/api/suoritusarvioinnit/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateSuoritusarviointi() {
        // Initialize the database
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val databaseSizeBeforeUpdate = suoritusarviointiRepository.findAll().size

        // Update the suoritusarviointi
        val id = suoritusarviointi.id
        assertNotNull(id)
        val updatedSuoritusarviointi = suoritusarviointiRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedSuoritusarviointi are not directly saved in db
        em.detach(updatedSuoritusarviointi)
        updatedSuoritusarviointi.tapahtumanAjankohta = UPDATED_TAPAHTUMAN_AJANKOHTA
        updatedSuoritusarviointi.arvioitavaTapahtuma = UPDATED_ARVIOITAVA_TAPAHTUMA
        updatedSuoritusarviointi.pyynnonAika = UPDATED_PYYNNON_AIKA
        updatedSuoritusarviointi.lisatiedot = UPDATED_LISATIEDOT
        updatedSuoritusarviointi.vaativuustaso = UPDATED_VAATIVUUSTASO
        updatedSuoritusarviointi.sanallinenArviointi = UPDATED_SANALLINEN_ARVIOINTI
        updatedSuoritusarviointi.arviointiAika = UPDATED_ARVIOINTI_AIKA
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(updatedSuoritusarviointi)

        restSuoritusarviointiMockMvc.perform(
            put("/api/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
        ).andExpect(status().isOk)

        // Validate the Suoritusarviointi in the database
        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testSuoritusarviointi = suoritusarviointiList[suoritusarviointiList.size - 1]
        assertThat(testSuoritusarviointi.tapahtumanAjankohta).isEqualTo(UPDATED_TAPAHTUMAN_AJANKOHTA)
        assertThat(testSuoritusarviointi.arvioitavaTapahtuma).isEqualTo(UPDATED_ARVIOITAVA_TAPAHTUMA)
        assertThat(testSuoritusarviointi.pyynnonAika).isEqualTo(UPDATED_PYYNNON_AIKA)
        assertThat(testSuoritusarviointi.lisatiedot).isEqualTo(UPDATED_LISATIEDOT)
        assertThat(testSuoritusarviointi.vaativuustaso).isEqualTo(UPDATED_VAATIVUUSTASO)
        assertThat(testSuoritusarviointi.sanallinenArviointi).isEqualTo(UPDATED_SANALLINEN_ARVIOINTI)
        assertThat(testSuoritusarviointi.arviointiAika).isEqualTo(UPDATED_ARVIOINTI_AIKA)
    }

    @Test
    @Transactional
    fun updateNonExistingSuoritusarviointi() {
        val databaseSizeBeforeUpdate = suoritusarviointiRepository.findAll().size

        // Create the Suoritusarviointi
        val suoritusarviointiDTO = suoritusarviointiMapper.toDto(suoritusarviointi)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSuoritusarviointiMockMvc.perform(
            put("/api/suoritusarvioinnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritusarviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Suoritusarviointi in the database
        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteSuoritusarviointi() {
        // Initialize the database
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        val databaseSizeBeforeDelete = suoritusarviointiRepository.findAll().size

        // Delete the suoritusarviointi
        restSuoritusarviointiMockMvc.perform(
            delete("/api/suoritusarvioinnit/{id}", suoritusarviointi.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val suoritusarviointiList = suoritusarviointiRepository.findAll()
        assertThat(suoritusarviointiList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private val DEFAULT_TAPAHTUMAN_AJANKOHTA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_TAPAHTUMAN_AJANKOHTA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_ARVIOITAVA_TAPAHTUMA = "AAAAAAAAAA"
        private const val UPDATED_ARVIOITAVA_TAPAHTUMA = "BBBBBBBBBB"

        private val DEFAULT_PYYNNON_AIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PYYNNON_AIKA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LISATIEDOT = "AAAAAAAAAA"
        private const val UPDATED_LISATIEDOT = "BBBBBBBBBB"

        private const val DEFAULT_ITSEARVIOINTI_VAATIVUUSTASO: Int = 1
        private const val UPDATED_ITSEARVIOINTI_VAATIVUUSTASO: Int = 2

        private const val DEFAULT_ITSEARVIOINTI_LUOTTAMUKSEN_TASO: Int = 1
        private const val UPDATED_ITSEARVIOINTI_LUOTTAMUKSEN_TASO: Int = 2

        private const val DEFAULT_SANALLINEN_ITSEARVIOINTI = "AAAAAAAAAA"
        private const val UPDATED_SANALLINEN_ITSEARVIOINTI = "BBBBBBBBBB"

        private const val DEFAULT_VAATIVUUSTASO: Int = 1
        private const val UPDATED_VAATIVUUSTASO: Int = 2

        private const val DEFAULT_LUOTTAMUKSEN_TASO: Int = 1
        private const val UPDATED_LUOTTAMUKSEN_TASO: Int = 2

        private const val DEFAULT_SANALLINEN_ARVIOINTI = "AAAAAAAAAA"
        private const val UPDATED_SANALLINEN_ARVIOINTI = "BBBBBBBBBB"

        private val DEFAULT_ARVIOINTI_AIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ARVIOINTI_AIKA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LUKITTU: Boolean = false
        private const val UPDATED_LUKITTU: Boolean = true

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = DEFAULT_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = DEFAULT_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = DEFAULT_PYYNNON_AIKA,
                lisatiedot = DEFAULT_LISATIEDOT,
                itsearviointiVaativuustaso = DEFAULT_ITSEARVIOINTI_VAATIVUUSTASO,
                itsearviointiLuottamuksenTaso = DEFAULT_ITSEARVIOINTI_LUOTTAMUKSEN_TASO,
                sanallinenItsearviointi = DEFAULT_SANALLINEN_ITSEARVIOINTI,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                luottamuksenTaso = DEFAULT_LUOTTAMUKSEN_TASO,
                sanallinenArviointi = DEFAULT_SANALLINEN_ARVIOINTI,
                arviointiAika = DEFAULT_ARVIOINTI_AIKA,
                lukittu = DEFAULT_LUKITTU
            )

            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaResourceIT.createEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Add required entity
            val epaOsaamisalue: EpaOsaamisalue
            if (em.findAll(EpaOsaamisalue::class).isEmpty()) {
                epaOsaamisalue = EpaOsaamisalueResourceIT.createEntity(em)
                em.persist(epaOsaamisalue)
                em.flush()
            } else {
                epaOsaamisalue = em.findAll(EpaOsaamisalue::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = epaOsaamisalue

            // Add required entity
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoResourceIT.createEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritusarviointi.tyoskentelyjakso = tyoskentelyjakso

            return suoritusarviointi
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Suoritusarviointi {
            val suoritusarviointi = Suoritusarviointi(
                tapahtumanAjankohta = UPDATED_TAPAHTUMAN_AJANKOHTA,
                arvioitavaTapahtuma = UPDATED_ARVIOITAVA_TAPAHTUMA,
                pyynnonAika = UPDATED_PYYNNON_AIKA,
                lisatiedot = UPDATED_LISATIEDOT,
                itsearviointiVaativuustaso = UPDATED_ITSEARVIOINTI_VAATIVUUSTASO,
                itsearviointiLuottamuksenTaso = UPDATED_ITSEARVIOINTI_LUOTTAMUKSEN_TASO,
                sanallinenItsearviointi = UPDATED_SANALLINEN_ITSEARVIOINTI,
                vaativuustaso = UPDATED_VAATIVUUSTASO,
                luottamuksenTaso = UPDATED_LUOTTAMUKSEN_TASO,
                sanallinenArviointi = UPDATED_SANALLINEN_ARVIOINTI,
                arviointiAika = UPDATED_ARVIOINTI_AIKA,
                lukittu = UPDATED_LUKITTU
            )

            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaResourceIT.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            suoritusarviointi.arvioinninAntaja = kayttaja

            // Add required entity
            val epaOsaamisalue: EpaOsaamisalue
            if (em.findAll(EpaOsaamisalue::class).isEmpty()) {
                epaOsaamisalue = EpaOsaamisalueResourceIT.createUpdatedEntity(em)
                em.persist(epaOsaamisalue)
                em.flush()
            } else {
                epaOsaamisalue = em.findAll(EpaOsaamisalue::class).get(0)
            }
            suoritusarviointi.arvioitavaOsaalue = epaOsaamisalue

            // Add required entity
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoResourceIT.createUpdatedEntity(em)
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
