package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
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
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

/**
 * Integration tests for the [TyoskentelyjaksoResource] REST controller.
 *
 * @see TyoskentelyjaksoResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class TyoskentelyjaksoResourceIT {

    @Autowired
    private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository

    @Autowired
    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

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

    private lateinit var restTyoskentelyjaksoMockMvc: MockMvc

    private lateinit var tyoskentelyjakso: Tyoskentelyjakso

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val tyoskentelyjaksoResource = TyoskentelyjaksoResource(tyoskentelyjaksoService)
        this.restTyoskentelyjaksoMockMvc = MockMvcBuilders.standaloneSetup(tyoskentelyjaksoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        tyoskentelyjakso = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createTyoskentelyjakso() {
        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        // Create the Tyoskentelyjakso
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
        ).andExpect(status().isCreated)

        // Validate the Tyoskentelyjakso in the database
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate + 1)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.tunnus).isEqualTo(DEFAULT_TUNNUS)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(DEFAULT_OSAAIKAPROSENTTI)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithExistingId() {
        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        // Create the Tyoskentelyjakso with an existing ID
        tyoskentelyjakso.id = 1L
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)

        // An entity with an existing ID cannot be created, so this API call must fail
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Tyoskentelyjakso in the database
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkTunnusIsRequired() {
        val databaseSizeBeforeTest = tyoskentelyjaksoRepository.findAll().size
        // set the field null
        tyoskentelyjakso.tunnus = null

        // Create the Tyoskentelyjakso, which fails.
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(
            post("/api/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkOsaaikaprosenttiIsRequired() {
        val databaseSizeBeforeTest = tyoskentelyjaksoRepository.findAll().size
        // set the field null
        tyoskentelyjakso.osaaikaprosentti = null

        // Create the Tyoskentelyjakso, which fails.
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(
            post("/api/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeTest)
    }


    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllTyoskentelyjaksot() {
        // Initialize the database
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        // Get all the tyoskentelyjaksoList
        restTyoskentelyjaksoMockMvc.perform(get("/api/tyoskentelyjaksot?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tyoskentelyjakso.id?.toInt())))
            .andExpect(jsonPath("$.[*].tunnus").value(hasItem(DEFAULT_TUNNUS)))
            .andExpect(jsonPath("$.[*].alkamispaiva").value(hasItem(DEFAULT_ALKAMISPAIVA.toString())))
            .andExpect(jsonPath("$.[*].paattymispaiva").value(hasItem(DEFAULT_PAATTYMISPAIVA.toString())))
            .andExpect(jsonPath("$.[*].osaaikaprosentti").value(hasItem(DEFAULT_OSAAIKAPROSENTTI)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getTyoskentelyjakso() {
        // Initialize the database
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        // Get the tyoskentelyjakso
        restTyoskentelyjaksoMockMvc.perform(get("/api/tyoskentelyjaksot/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tyoskentelyjakso.id as Any))
            .andExpect(jsonPath("$.tunnus").value(DEFAULT_TUNNUS))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.osaaikaprosentti").value(DEFAULT_OSAAIKAPROSENTTI))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingTyoskentelyjakso() {
        // Get the tyoskentelyjakso
        restTyoskentelyjaksoMockMvc.perform(get("/api/tyoskentelyjaksot/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateTyoskentelyjakso() {
        // Initialize the database
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        // Update the tyoskentelyjakso
        val id = tyoskentelyjakso.id
        assertNotNull(id)
        val updatedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedTyoskentelyjakso are not directly saved in db
        em.detach(updatedTyoskentelyjakso)
        updatedTyoskentelyjakso.tunnus = UPDATED_TUNNUS
        updatedTyoskentelyjakso.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTyoskentelyjakso.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTyoskentelyjakso.osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(updatedTyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
        ).andExpect(status().isOk)

        // Validate the Tyoskentelyjakso in the database
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.tunnus).isEqualTo(UPDATED_TUNNUS)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(UPDATED_OSAAIKAPROSENTTI)
    }

    @Test
    @Transactional
    fun updateNonExistingTyoskentelyjakso() {
        val databaseSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        // Create the Tyoskentelyjakso
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTyoskentelyjaksoMockMvc.perform(
            put("/api/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Tyoskentelyjakso in the database
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteTyoskentelyjakso() {
        // Initialize the database
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        // Delete the tyoskentelyjakso
        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_TUNNUS = "AAAAAAAAAA"
        private const val UPDATED_TUNNUS = "BBBBBBBBBB"

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_OSAAIKAPROSENTTI: Int = 50
        private const val UPDATED_OSAAIKAPROSENTTI: Int = 51

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                tunnus = DEFAULT_TUNNUS,
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI
            )

            // Add required entity
            val tyoskentelypaikka: Tyoskentelypaikka
            if (em.findAll(Tyoskentelypaikka::class).isEmpty()) {
                tyoskentelypaikka = TyoskentelypaikkaResourceIT.createEntity()
                em.persist(tyoskentelypaikka)
                em.flush()
            } else {
                tyoskentelypaikka = em.findAll(Tyoskentelypaikka::class).get(0)
            }
            tyoskentelyjakso.tyoskentelypaikka = tyoskentelypaikka

            return tyoskentelyjakso
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                tunnus = UPDATED_TUNNUS,
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
            )

            // Add required entity
            val tyoskentelypaikka: Tyoskentelypaikka
            if (em.findAll(Tyoskentelypaikka::class).isEmpty()) {
                tyoskentelypaikka = TyoskentelypaikkaResourceIT.createUpdatedEntity()
                em.persist(tyoskentelypaikka)
                em.flush()
            } else {
                tyoskentelypaikka = em.findAll(Tyoskentelypaikka::class).get(0)
            }
            tyoskentelyjakso.tyoskentelypaikka = tyoskentelypaikka

            return tyoskentelyjakso
        }
    }
}
