package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.OsaalueenArviointi
import fi.elsapalvelu.elsa.repository.OsaalueenArviointiRepository
import fi.elsapalvelu.elsa.service.OsaalueenArviointiService
import fi.elsapalvelu.elsa.service.mapper.OsaalueenArviointiMapper
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
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
 * Integration tests for the [OsaalueenArviointiResource] REST controller.
 *
 * @see OsaalueenArviointiResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class OsaalueenArviointiResourceIT {

    @Autowired
    private lateinit var osaalueenArviointiRepository: OsaalueenArviointiRepository

    @Autowired
    private lateinit var osaalueenArviointiMapper: OsaalueenArviointiMapper

    @Autowired
    private lateinit var osaalueenArviointiService: OsaalueenArviointiService

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

    private lateinit var restOsaalueenArviointiMockMvc: MockMvc

    private lateinit var osaalueenArviointi: OsaalueenArviointi

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val osaalueenArviointiResource = OsaalueenArviointiResource(osaalueenArviointiService)
         this.restOsaalueenArviointiMockMvc = MockMvcBuilders.standaloneSetup(osaalueenArviointiResource)
             .setCustomArgumentResolvers(pageableArgumentResolver)
             .setControllerAdvice(exceptionTranslator)
             .setConversionService(createFormattingConversionService())
             .setMessageConverters(jacksonMessageConverter)
             .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        osaalueenArviointi = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createOsaalueenArviointi() {
        val databaseSizeBeforeCreate = osaalueenArviointiRepository.findAll().size

        // Create the OsaalueenArviointi
        val osaalueenArviointiDTO = osaalueenArviointiMapper.toDto(osaalueenArviointi)
        restOsaalueenArviointiMockMvc.perform(
            post("/api/osaalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaalueenArviointiDTO))
        ).andExpect(status().isCreated)

        // Validate the OsaalueenArviointi in the database
        val osaalueenArviointiList = osaalueenArviointiRepository.findAll()
        assertThat(osaalueenArviointiList).hasSize(databaseSizeBeforeCreate + 1)
        val testOsaalueenArviointi = osaalueenArviointiList[osaalueenArviointiList.size - 1]
        assertThat(testOsaalueenArviointi.arvio).isEqualTo(DEFAULT_ARVIO)
    }

    @Test
    @Transactional
    fun createOsaalueenArviointiWithExistingId() {
        val databaseSizeBeforeCreate = osaalueenArviointiRepository.findAll().size

        // Create the OsaalueenArviointi with an existing ID
        osaalueenArviointi.id = 1L
        val osaalueenArviointiDTO = osaalueenArviointiMapper.toDto(osaalueenArviointi)

        // An entity with an existing ID cannot be created, so this API call must fail
        restOsaalueenArviointiMockMvc.perform(
            post("/api/osaalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaalueenArviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the OsaalueenArviointi in the database
        val osaalueenArviointiList = osaalueenArviointiRepository.findAll()
        assertThat(osaalueenArviointiList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllOsaalueenArviointis() {
        // Initialize the database
        osaalueenArviointiRepository.saveAndFlush(osaalueenArviointi)

        // Get all the osaalueenArviointiList
        restOsaalueenArviointiMockMvc.perform(get("/api/osaalueen-arviointis?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(osaalueenArviointi.id?.toInt())))
            .andExpect(jsonPath("$.[*].arvio").value(hasItem(DEFAULT_ARVIO))) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getOsaalueenArviointi() {
        // Initialize the database
        osaalueenArviointiRepository.saveAndFlush(osaalueenArviointi)

        val id = osaalueenArviointi.id
        assertNotNull(id)

        // Get the osaalueenArviointi
        restOsaalueenArviointiMockMvc.perform(get("/api/osaalueen-arviointis/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(osaalueenArviointi.id as Any))
            .andExpect(jsonPath("$.arvio").value(DEFAULT_ARVIO)) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingOsaalueenArviointi() {
        // Get the osaalueenArviointi
        restOsaalueenArviointiMockMvc.perform(get("/api/osaalueen-arviointis/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateOsaalueenArviointi() {
        // Initialize the database
        osaalueenArviointiRepository.saveAndFlush(osaalueenArviointi)

        val databaseSizeBeforeUpdate = osaalueenArviointiRepository.findAll().size

        // Update the osaalueenArviointi
        val id = osaalueenArviointi.id
        assertNotNull(id)
        val updatedOsaalueenArviointi = osaalueenArviointiRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedOsaalueenArviointi are not directly saved in db
        em.detach(updatedOsaalueenArviointi)
        updatedOsaalueenArviointi.arvio = UPDATED_ARVIO
        val osaalueenArviointiDTO = osaalueenArviointiMapper.toDto(updatedOsaalueenArviointi)

        restOsaalueenArviointiMockMvc.perform(
            put("/api/osaalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaalueenArviointiDTO))
        ).andExpect(status().isOk)

        // Validate the OsaalueenArviointi in the database
        val osaalueenArviointiList = osaalueenArviointiRepository.findAll()
        assertThat(osaalueenArviointiList).hasSize(databaseSizeBeforeUpdate)
        val testOsaalueenArviointi = osaalueenArviointiList[osaalueenArviointiList.size - 1]
        assertThat(testOsaalueenArviointi.arvio).isEqualTo(UPDATED_ARVIO)
    }

    @Test
    @Transactional
    fun updateNonExistingOsaalueenArviointi() {
        val databaseSizeBeforeUpdate = osaalueenArviointiRepository.findAll().size

        // Create the OsaalueenArviointi
        val osaalueenArviointiDTO = osaalueenArviointiMapper.toDto(osaalueenArviointi)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOsaalueenArviointiMockMvc.perform(
            put("/api/osaalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaalueenArviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the OsaalueenArviointi in the database
        val osaalueenArviointiList = osaalueenArviointiRepository.findAll()
        assertThat(osaalueenArviointiList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteOsaalueenArviointi() {
        // Initialize the database
        osaalueenArviointiRepository.saveAndFlush(osaalueenArviointi)

        val databaseSizeBeforeDelete = osaalueenArviointiRepository.findAll().size

        // Delete the osaalueenArviointi
        restOsaalueenArviointiMockMvc.perform(
            delete("/api/osaalueen-arviointis/{id}", osaalueenArviointi.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val osaalueenArviointiList = osaalueenArviointiRepository.findAll()
        assertThat(osaalueenArviointiList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_ARVIO: Int = 1
        private const val UPDATED_ARVIO: Int = 2

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): OsaalueenArviointi {
            val osaalueenArviointi = OsaalueenArviointi(
                arvio = DEFAULT_ARVIO
            )

            return osaalueenArviointi
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): OsaalueenArviointi {
            val osaalueenArviointi = OsaalueenArviointi(
                arvio = UPDATED_ARVIO
            )

            return osaalueenArviointi
        }
    }
}
