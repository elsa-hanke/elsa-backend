package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Pikaviesti
import fi.oulu.elsa.repository.PikaviestiRepository
import fi.oulu.elsa.service.PikaviestiService
import fi.oulu.elsa.service.mapper.PikaviestiMapper
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
 * Integration tests for the [PikaviestiResource] REST controller.
 *
 * @see PikaviestiResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class PikaviestiResourceIT {

    @Autowired
    private lateinit var pikaviestiRepository: PikaviestiRepository

    @Autowired
    private lateinit var pikaviestiMapper: PikaviestiMapper

    @Autowired
    private lateinit var pikaviestiService: PikaviestiService

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

    private lateinit var restPikaviestiMockMvc: MockMvc

    private lateinit var pikaviesti: Pikaviesti

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val pikaviestiResource = PikaviestiResource(pikaviestiService)
        this.restPikaviestiMockMvc = MockMvcBuilders.standaloneSetup(pikaviestiResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        pikaviesti = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createPikaviesti() {
        val databaseSizeBeforeCreate = pikaviestiRepository.findAll().size

        // Create the Pikaviesti
        val pikaviestiDTO = pikaviestiMapper.toDto(pikaviesti)
        restPikaviestiMockMvc.perform(
            post("/api/pikaviestis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiDTO))
        ).andExpect(status().isCreated)

        // Validate the Pikaviesti in the database
        val pikaviestiList = pikaviestiRepository.findAll()
        assertThat(pikaviestiList).hasSize(databaseSizeBeforeCreate + 1)
        val testPikaviesti = pikaviestiList[pikaviestiList.size - 1]
        assertThat(testPikaviesti.sisalto).isEqualTo(DEFAULT_SISALTO)
    }

    @Test
    @Transactional
    fun createPikaviestiWithExistingId() {
        val databaseSizeBeforeCreate = pikaviestiRepository.findAll().size

        // Create the Pikaviesti with an existing ID
        pikaviesti.id = 1L
        val pikaviestiDTO = pikaviestiMapper.toDto(pikaviesti)

        // An entity with an existing ID cannot be created, so this API call must fail
        restPikaviestiMockMvc.perform(
            post("/api/pikaviestis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Pikaviesti in the database
        val pikaviestiList = pikaviestiRepository.findAll()
        assertThat(pikaviestiList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPikaviestis() {
        // Initialize the database
        pikaviestiRepository.saveAndFlush(pikaviesti)

        // Get all the pikaviestiList
        restPikaviestiMockMvc.perform(get("/api/pikaviestis?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pikaviesti.id?.toInt())))
            .andExpect(jsonPath("$.[*].sisalto").value(hasItem(DEFAULT_SISALTO)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getPikaviesti() {
        // Initialize the database
        pikaviestiRepository.saveAndFlush(pikaviesti)

        val id = pikaviesti.id
        assertNotNull(id)

        // Get the pikaviesti
        restPikaviestiMockMvc.perform(get("/api/pikaviestis/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pikaviesti.id?.toInt()))
            .andExpect(jsonPath("$.sisalto").value(DEFAULT_SISALTO))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingPikaviesti() {
        // Get the pikaviesti
        restPikaviestiMockMvc.perform(get("/api/pikaviestis/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updatePikaviesti() {
        // Initialize the database
        pikaviestiRepository.saveAndFlush(pikaviesti)

        val databaseSizeBeforeUpdate = pikaviestiRepository.findAll().size

        // Update the pikaviesti
        val id = pikaviesti.id
        assertNotNull(id)
        val updatedPikaviesti = pikaviestiRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedPikaviesti are not directly saved in db
        em.detach(updatedPikaviesti)
        updatedPikaviesti.sisalto = UPDATED_SISALTO
        val pikaviestiDTO = pikaviestiMapper.toDto(updatedPikaviesti)

        restPikaviestiMockMvc.perform(
            put("/api/pikaviestis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiDTO))
        ).andExpect(status().isOk)

        // Validate the Pikaviesti in the database
        val pikaviestiList = pikaviestiRepository.findAll()
        assertThat(pikaviestiList).hasSize(databaseSizeBeforeUpdate)
        val testPikaviesti = pikaviestiList[pikaviestiList.size - 1]
        assertThat(testPikaviesti.sisalto).isEqualTo(UPDATED_SISALTO)
    }

    @Test
    @Transactional
    fun updateNonExistingPikaviesti() {
        val databaseSizeBeforeUpdate = pikaviestiRepository.findAll().size

        // Create the Pikaviesti
        val pikaviestiDTO = pikaviestiMapper.toDto(pikaviesti)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPikaviestiMockMvc.perform(
            put("/api/pikaviestis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Pikaviesti in the database
        val pikaviestiList = pikaviestiRepository.findAll()
        assertThat(pikaviestiList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deletePikaviesti() {
        // Initialize the database
        pikaviestiRepository.saveAndFlush(pikaviesti)

        val databaseSizeBeforeDelete = pikaviestiRepository.findAll().size

        // Delete the pikaviesti
        restPikaviestiMockMvc.perform(
            delete("/api/pikaviestis/{id}", pikaviesti.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val pikaviestiList = pikaviestiRepository.findAll()
        assertThat(pikaviestiList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_SISALTO = "AAAAAAAAAA"
        private const val UPDATED_SISALTO = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Pikaviesti {
            val pikaviesti = Pikaviesti(
                sisalto = DEFAULT_SISALTO
            )

            return pikaviesti
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Pikaviesti {
            val pikaviesti = Pikaviesti(
                sisalto = UPDATED_SISALTO
            )

            return pikaviesti
        }
    }
}
