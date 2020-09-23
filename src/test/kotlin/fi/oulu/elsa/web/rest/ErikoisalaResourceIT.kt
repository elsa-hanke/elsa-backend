package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Erikoisala
import fi.oulu.elsa.repository.ErikoisalaRepository
import fi.oulu.elsa.service.ErikoisalaService
import fi.oulu.elsa.service.mapper.ErikoisalaMapper
import fi.oulu.elsa.web.rest.errors.ExceptionTranslator
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
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

/**
 * Integration tests for the [ErikoisalaResource] REST controller.
 *
 * @see ErikoisalaResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class ErikoisalaResourceIT {

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var erikoisalaMapper: ErikoisalaMapper

    @Autowired
    private lateinit var erikoisalaService: ErikoisalaService

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

    private lateinit var restErikoisalaMockMvc: MockMvc

    private lateinit var erikoisala: Erikoisala

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val erikoisalaResource = ErikoisalaResource(erikoisalaService)
        this.restErikoisalaMockMvc = MockMvcBuilders.standaloneSetup(erikoisalaResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        erikoisala = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createErikoisala() {
        val databaseSizeBeforeCreate = erikoisalaRepository.findAll().size

        // Create the Erikoisala
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        restErikoisalaMockMvc.perform(
            post("/api/erikoisalas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoisalaDTO))
        ).andExpect(status().isCreated)

        // Validate the Erikoisala in the database
        val erikoisalaList = erikoisalaRepository.findAll()
        assertThat(erikoisalaList).hasSize(databaseSizeBeforeCreate + 1)
        val testErikoisala = erikoisalaList[erikoisalaList.size - 1]
        assertThat(testErikoisala.nimi).isEqualTo(DEFAULT_NIMI)
    }

    @Test
    @Transactional
    fun createErikoisalaWithExistingId() {
        val databaseSizeBeforeCreate = erikoisalaRepository.findAll().size

        // Create the Erikoisala with an existing ID
        erikoisala.id = 1L
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)

        // An entity with an existing ID cannot be created, so this API call must fail
        restErikoisalaMockMvc.perform(
            post("/api/erikoisalas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoisalaDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Erikoisala in the database
        val erikoisalaList = erikoisalaRepository.findAll()
        assertThat(erikoisalaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkNimiIsRequired() {
        val databaseSizeBeforeTest = erikoisalaRepository.findAll().size
        // set the field null
        erikoisala.nimi = null

        // Create the Erikoisala, which fails.
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)

        restErikoisalaMockMvc.perform(
            post("/api/erikoisalas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoisalaDTO))
        ).andExpect(status().isBadRequest)

        val erikoisalaList = erikoisalaRepository.findAll()
        assertThat(erikoisalaList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllErikoisalas() {
        // Initialize the database
        erikoisalaRepository.saveAndFlush(erikoisala)

        // Get all the erikoisalaList
        restErikoisalaMockMvc.perform(get("/api/erikoisalas?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(erikoisala.id?.toInt())))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getErikoisala() {
        // Initialize the database
        erikoisalaRepository.saveAndFlush(erikoisala)

        val id = erikoisala.id
        assertNotNull(id)

        // Get the erikoisala
        restErikoisalaMockMvc.perform(get("/api/erikoisalas/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(erikoisala.id?.toInt()))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingErikoisala() {
        // Get the erikoisala
        restErikoisalaMockMvc.perform(get("/api/erikoisalas/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateErikoisala() {
        // Initialize the database
        erikoisalaRepository.saveAndFlush(erikoisala)

        val databaseSizeBeforeUpdate = erikoisalaRepository.findAll().size

        // Update the erikoisala
        val id = erikoisala.id
        assertNotNull(id)
        val updatedErikoisala = erikoisalaRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedErikoisala are not directly saved in db
        em.detach(updatedErikoisala)
        updatedErikoisala.nimi = UPDATED_NIMI
        val erikoisalaDTO = erikoisalaMapper.toDto(updatedErikoisala)

        restErikoisalaMockMvc.perform(
            put("/api/erikoisalas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoisalaDTO))
        ).andExpect(status().isOk)

        // Validate the Erikoisala in the database
        val erikoisalaList = erikoisalaRepository.findAll()
        assertThat(erikoisalaList).hasSize(databaseSizeBeforeUpdate)
        val testErikoisala = erikoisalaList[erikoisalaList.size - 1]
        assertThat(testErikoisala.nimi).isEqualTo(UPDATED_NIMI)
    }

    @Test
    @Transactional
    fun updateNonExistingErikoisala() {
        val databaseSizeBeforeUpdate = erikoisalaRepository.findAll().size

        // Create the Erikoisala
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restErikoisalaMockMvc.perform(
            put("/api/erikoisalas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoisalaDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Erikoisala in the database
        val erikoisalaList = erikoisalaRepository.findAll()
        assertThat(erikoisalaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteErikoisala() {
        // Initialize the database
        erikoisalaRepository.saveAndFlush(erikoisala)

        val databaseSizeBeforeDelete = erikoisalaRepository.findAll().size

        // Delete the erikoisala
        restErikoisalaMockMvc.perform(
            delete("/api/erikoisalas/{id}", erikoisala.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val erikoisalaList = erikoisalaRepository.findAll()
        assertThat(erikoisalaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = DEFAULT_NIMI
            )

            return erikoisala
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = UPDATED_NIMI
            )

            return erikoisala
        }
    }
}
