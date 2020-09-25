package fi.oulu.elsa.web.rest

import fi.oulu.elsa.ElsaBackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Hops
import fi.oulu.elsa.repository.HopsRepository
import fi.oulu.elsa.service.HopsService
import fi.oulu.elsa.service.mapper.HopsMapper
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
 * Integration tests for the [HopsResource] REST controller.
 *
 * @see HopsResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class HopsResourceIT {

    @Autowired
    private lateinit var hopsRepository: HopsRepository

    @Autowired
    private lateinit var hopsMapper: HopsMapper

    @Autowired
    private lateinit var hopsService: HopsService

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

    private lateinit var restHopsMockMvc: MockMvc

    private lateinit var hops: Hops

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val hopsResource = HopsResource(hopsService)
        this.restHopsMockMvc = MockMvcBuilders.standaloneSetup(hopsResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        hops = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createHops() {
        val databaseSizeBeforeCreate = hopsRepository.findAll().size

        // Create the Hops
        val hopsDTO = hopsMapper.toDto(hops)
        restHopsMockMvc.perform(
            post("/api/hops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(hopsDTO))
        ).andExpect(status().isCreated)

        // Validate the Hops in the database
        val hopsList = hopsRepository.findAll()
        assertThat(hopsList).hasSize(databaseSizeBeforeCreate + 1)
        val testHops = hopsList[hopsList.size - 1]
        assertThat(testHops.suunnitelmanTunnus).isEqualTo(DEFAULT_SUUNNITELMAN_TUNNUS)
    }

    @Test
    @Transactional
    fun createHopsWithExistingId() {
        val databaseSizeBeforeCreate = hopsRepository.findAll().size

        // Create the Hops with an existing ID
        hops.id = 1L
        val hopsDTO = hopsMapper.toDto(hops)

        // An entity with an existing ID cannot be created, so this API call must fail
        restHopsMockMvc.perform(
            post("/api/hops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(hopsDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Hops in the database
        val hopsList = hopsRepository.findAll()
        assertThat(hopsList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllHops() {
        // Initialize the database
        hopsRepository.saveAndFlush(hops)

        // Get all the hopsList
        restHopsMockMvc.perform(get("/api/hops?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(hops.id?.toInt())))
            .andExpect(jsonPath("$.[*].suunnitelmanTunnus").value(hasItem(DEFAULT_SUUNNITELMAN_TUNNUS)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getHops() {
        // Initialize the database
        hopsRepository.saveAndFlush(hops)

        val id = hops.id
        assertNotNull(id)

        // Get the hops
        restHopsMockMvc.perform(get("/api/hops/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(hops.id?.toInt()))
            .andExpect(jsonPath("$.suunnitelmanTunnus").value(DEFAULT_SUUNNITELMAN_TUNNUS))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingHops() {
        // Get the hops
        restHopsMockMvc.perform(get("/api/hops/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateHops() {
        // Initialize the database
        hopsRepository.saveAndFlush(hops)

        val databaseSizeBeforeUpdate = hopsRepository.findAll().size

        // Update the hops
        val id = hops.id
        assertNotNull(id)
        val updatedHops = hopsRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedHops are not directly saved in db
        em.detach(updatedHops)
        updatedHops.suunnitelmanTunnus = UPDATED_SUUNNITELMAN_TUNNUS
        val hopsDTO = hopsMapper.toDto(updatedHops)

        restHopsMockMvc.perform(
            put("/api/hops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(hopsDTO))
        ).andExpect(status().isOk)

        // Validate the Hops in the database
        val hopsList = hopsRepository.findAll()
        assertThat(hopsList).hasSize(databaseSizeBeforeUpdate)
        val testHops = hopsList[hopsList.size - 1]
        assertThat(testHops.suunnitelmanTunnus).isEqualTo(UPDATED_SUUNNITELMAN_TUNNUS)
    }

    @Test
    @Transactional
    fun updateNonExistingHops() {
        val databaseSizeBeforeUpdate = hopsRepository.findAll().size

        // Create the Hops
        val hopsDTO = hopsMapper.toDto(hops)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restHopsMockMvc.perform(
            put("/api/hops")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(hopsDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Hops in the database
        val hopsList = hopsRepository.findAll()
        assertThat(hopsList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteHops() {
        // Initialize the database
        hopsRepository.saveAndFlush(hops)

        val databaseSizeBeforeDelete = hopsRepository.findAll().size

        // Delete the hops
        restHopsMockMvc.perform(
            delete("/api/hops/{id}", hops.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val hopsList = hopsRepository.findAll()
        assertThat(hopsList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_SUUNNITELMAN_TUNNUS = "AAAAAAAAAA"
        private const val UPDATED_SUUNNITELMAN_TUNNUS = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Hops {
            val hops = Hops(
                suunnitelmanTunnus = DEFAULT_SUUNNITELMAN_TUNNUS
            )

            return hops
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Hops {
            val hops = Hops(
                suunnitelmanTunnus = UPDATED_SUUNNITELMAN_TUNNUS
            )

            return hops
        }
    }
}
