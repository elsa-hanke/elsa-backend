package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.OsaamisalueenArviointi
import fi.oulu.elsa.repository.OsaamisalueenArviointiRepository
import fi.oulu.elsa.service.OsaamisalueenArviointiService
import fi.oulu.elsa.service.mapper.OsaamisalueenArviointiMapper
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
 * Integration tests for the [OsaamisalueenArviointiResource] REST controller.
 *
 * @see OsaamisalueenArviointiResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class OsaamisalueenArviointiResourceIT {

    @Autowired
    private lateinit var osaamisalueenArviointiRepository: OsaamisalueenArviointiRepository

    @Autowired
    private lateinit var osaamisalueenArviointiMapper: OsaamisalueenArviointiMapper

    @Autowired
    private lateinit var osaamisalueenArviointiService: OsaamisalueenArviointiService

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

    private lateinit var restOsaamisalueenArviointiMockMvc: MockMvc

    private lateinit var osaamisalueenArviointi: OsaamisalueenArviointi

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val osaamisalueenArviointiResource = OsaamisalueenArviointiResource(osaamisalueenArviointiService)
        this.restOsaamisalueenArviointiMockMvc = MockMvcBuilders.standaloneSetup(osaamisalueenArviointiResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        osaamisalueenArviointi = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createOsaamisalueenArviointi() {
        val databaseSizeBeforeCreate = osaamisalueenArviointiRepository.findAll().size

        // Create the OsaamisalueenArviointi
        val osaamisalueenArviointiDTO = osaamisalueenArviointiMapper.toDto(osaamisalueenArviointi)
        restOsaamisalueenArviointiMockMvc.perform(
            post("/api/osaamisalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisalueenArviointiDTO))
        ).andExpect(status().isCreated)

        // Validate the OsaamisalueenArviointi in the database
        val osaamisalueenArviointiList = osaamisalueenArviointiRepository.findAll()
        assertThat(osaamisalueenArviointiList).hasSize(databaseSizeBeforeCreate + 1)
        val testOsaamisalueenArviointi = osaamisalueenArviointiList[osaamisalueenArviointiList.size - 1]
        assertThat(testOsaamisalueenArviointi.vaatimustaso).isEqualTo(DEFAULT_VAATIMUSTASO)
        assertThat(testOsaamisalueenArviointi.arvio).isEqualTo(DEFAULT_ARVIO)
        assertThat(testOsaamisalueenArviointi.sanallinenArvio).isEqualTo(DEFAULT_SANALLINEN_ARVIO)
    }

    @Test
    @Transactional
    fun createOsaamisalueenArviointiWithExistingId() {
        val databaseSizeBeforeCreate = osaamisalueenArviointiRepository.findAll().size

        // Create the OsaamisalueenArviointi with an existing ID
        osaamisalueenArviointi.id = 1L
        val osaamisalueenArviointiDTO = osaamisalueenArviointiMapper.toDto(osaamisalueenArviointi)

        // An entity with an existing ID cannot be created, so this API call must fail
        restOsaamisalueenArviointiMockMvc.perform(
            post("/api/osaamisalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisalueenArviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the OsaamisalueenArviointi in the database
        val osaamisalueenArviointiList = osaamisalueenArviointiRepository.findAll()
        assertThat(osaamisalueenArviointiList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllOsaamisalueenArviointis() {
        // Initialize the database
        osaamisalueenArviointiRepository.saveAndFlush(osaamisalueenArviointi)

        // Get all the osaamisalueenArviointiList
        restOsaamisalueenArviointiMockMvc.perform(get("/api/osaamisalueen-arviointis?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(osaamisalueenArviointi.id?.toInt())))
            .andExpect(jsonPath("$.[*].vaatimustaso").value(hasItem(DEFAULT_VAATIMUSTASO)))
            .andExpect(jsonPath("$.[*].arvio").value(hasItem(DEFAULT_ARVIO)))
            .andExpect(jsonPath("$.[*].sanallinenArvio").value(hasItem(DEFAULT_SANALLINEN_ARVIO)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getOsaamisalueenArviointi() {
        // Initialize the database
        osaamisalueenArviointiRepository.saveAndFlush(osaamisalueenArviointi)

        val id = osaamisalueenArviointi.id
        assertNotNull(id)

        // Get the osaamisalueenArviointi
        restOsaamisalueenArviointiMockMvc.perform(get("/api/osaamisalueen-arviointis/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(osaamisalueenArviointi.id?.toInt()))
            .andExpect(jsonPath("$.vaatimustaso").value(DEFAULT_VAATIMUSTASO))
            .andExpect(jsonPath("$.arvio").value(DEFAULT_ARVIO))
            .andExpect(jsonPath("$.sanallinenArvio").value(DEFAULT_SANALLINEN_ARVIO))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingOsaamisalueenArviointi() {
        // Get the osaamisalueenArviointi
        restOsaamisalueenArviointiMockMvc.perform(get("/api/osaamisalueen-arviointis/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateOsaamisalueenArviointi() {
        // Initialize the database
        osaamisalueenArviointiRepository.saveAndFlush(osaamisalueenArviointi)

        val databaseSizeBeforeUpdate = osaamisalueenArviointiRepository.findAll().size

        // Update the osaamisalueenArviointi
        val id = osaamisalueenArviointi.id
        assertNotNull(id)
        val updatedOsaamisalueenArviointi = osaamisalueenArviointiRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedOsaamisalueenArviointi are not directly saved in db
        em.detach(updatedOsaamisalueenArviointi)
        updatedOsaamisalueenArviointi.vaatimustaso = UPDATED_VAATIMUSTASO
        updatedOsaamisalueenArviointi.arvio = UPDATED_ARVIO
        updatedOsaamisalueenArviointi.sanallinenArvio = UPDATED_SANALLINEN_ARVIO
        val osaamisalueenArviointiDTO = osaamisalueenArviointiMapper.toDto(updatedOsaamisalueenArviointi)

        restOsaamisalueenArviointiMockMvc.perform(
            put("/api/osaamisalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisalueenArviointiDTO))
        ).andExpect(status().isOk)

        // Validate the OsaamisalueenArviointi in the database
        val osaamisalueenArviointiList = osaamisalueenArviointiRepository.findAll()
        assertThat(osaamisalueenArviointiList).hasSize(databaseSizeBeforeUpdate)
        val testOsaamisalueenArviointi = osaamisalueenArviointiList[osaamisalueenArviointiList.size - 1]
        assertThat(testOsaamisalueenArviointi.vaatimustaso).isEqualTo(UPDATED_VAATIMUSTASO)
        assertThat(testOsaamisalueenArviointi.arvio).isEqualTo(UPDATED_ARVIO)
        assertThat(testOsaamisalueenArviointi.sanallinenArvio).isEqualTo(UPDATED_SANALLINEN_ARVIO)
    }

    @Test
    @Transactional
    fun updateNonExistingOsaamisalueenArviointi() {
        val databaseSizeBeforeUpdate = osaamisalueenArviointiRepository.findAll().size

        // Create the OsaamisalueenArviointi
        val osaamisalueenArviointiDTO = osaamisalueenArviointiMapper.toDto(osaamisalueenArviointi)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOsaamisalueenArviointiMockMvc.perform(
            put("/api/osaamisalueen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisalueenArviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the OsaamisalueenArviointi in the database
        val osaamisalueenArviointiList = osaamisalueenArviointiRepository.findAll()
        assertThat(osaamisalueenArviointiList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteOsaamisalueenArviointi() {
        // Initialize the database
        osaamisalueenArviointiRepository.saveAndFlush(osaamisalueenArviointi)

        val databaseSizeBeforeDelete = osaamisalueenArviointiRepository.findAll().size

        // Delete the osaamisalueenArviointi
        restOsaamisalueenArviointiMockMvc.perform(
            delete("/api/osaamisalueen-arviointis/{id}", osaamisalueenArviointi.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val osaamisalueenArviointiList = osaamisalueenArviointiRepository.findAll()
        assertThat(osaamisalueenArviointiList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_VAATIMUSTASO: Int = 1
        private const val UPDATED_VAATIMUSTASO: Int = 2

        private const val DEFAULT_ARVIO: Int = 0
        private const val UPDATED_ARVIO: Int = 1

        private const val DEFAULT_SANALLINEN_ARVIO = "AAAAAAAAAA"
        private const val UPDATED_SANALLINEN_ARVIO = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): OsaamisalueenArviointi {
            val osaamisalueenArviointi = OsaamisalueenArviointi(
                vaatimustaso = DEFAULT_VAATIMUSTASO,
                arvio = DEFAULT_ARVIO,
                sanallinenArvio = DEFAULT_SANALLINEN_ARVIO
            )

            return osaamisalueenArviointi
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): OsaamisalueenArviointi {
            val osaamisalueenArviointi = OsaamisalueenArviointi(
                vaatimustaso = UPDATED_VAATIMUSTASO,
                arvio = UPDATED_ARVIO,
                sanallinenArvio = UPDATED_SANALLINEN_ARVIO
            )

            return osaamisalueenArviointi
        }
    }
}
