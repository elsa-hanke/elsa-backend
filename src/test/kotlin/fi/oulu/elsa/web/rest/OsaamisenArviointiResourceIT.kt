package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.OsaamisenArviointi
import fi.oulu.elsa.repository.OsaamisenArviointiRepository
import fi.oulu.elsa.service.OsaamisenArviointiService
import fi.oulu.elsa.service.mapper.OsaamisenArviointiMapper
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
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

/**
 * Integration tests for the [OsaamisenArviointiResource] REST controller.
 *
 * @see OsaamisenArviointiResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class OsaamisenArviointiResourceIT {

    @Autowired
    private lateinit var osaamisenArviointiRepository: OsaamisenArviointiRepository

    @Autowired
    private lateinit var osaamisenArviointiMapper: OsaamisenArviointiMapper

    @Autowired
    private lateinit var osaamisenArviointiService: OsaamisenArviointiService

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

    private lateinit var restOsaamisenArviointiMockMvc: MockMvc

    private lateinit var osaamisenArviointi: OsaamisenArviointi

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val osaamisenArviointiResource = OsaamisenArviointiResource(osaamisenArviointiService)
        this.restOsaamisenArviointiMockMvc = MockMvcBuilders.standaloneSetup(osaamisenArviointiResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        osaamisenArviointi = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createOsaamisenArviointi() {
        val databaseSizeBeforeCreate = osaamisenArviointiRepository.findAll().size

        // Create the OsaamisenArviointi
        val osaamisenArviointiDTO = osaamisenArviointiMapper.toDto(osaamisenArviointi)
        restOsaamisenArviointiMockMvc.perform(
            post("/api/osaamisen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisenArviointiDTO))
        ).andExpect(status().isCreated)

        // Validate the OsaamisenArviointi in the database
        val osaamisenArviointiList = osaamisenArviointiRepository.findAll()
        assertThat(osaamisenArviointiList).hasSize(databaseSizeBeforeCreate + 1)
        val testOsaamisenArviointi = osaamisenArviointiList[osaamisenArviointiList.size - 1]
        assertThat(testOsaamisenArviointi.tunnus).isEqualTo(DEFAULT_TUNNUS)
        assertThat(testOsaamisenArviointi.osasto).isEqualTo(DEFAULT_OSASTO)
        assertThat(testOsaamisenArviointi.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testOsaamisenArviointi.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
    }

    @Test
    @Transactional
    fun createOsaamisenArviointiWithExistingId() {
        val databaseSizeBeforeCreate = osaamisenArviointiRepository.findAll().size

        // Create the OsaamisenArviointi with an existing ID
        osaamisenArviointi.id = 1L
        val osaamisenArviointiDTO = osaamisenArviointiMapper.toDto(osaamisenArviointi)

        // An entity with an existing ID cannot be created, so this API call must fail
        restOsaamisenArviointiMockMvc.perform(
            post("/api/osaamisen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisenArviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the OsaamisenArviointi in the database
        val osaamisenArviointiList = osaamisenArviointiRepository.findAll()
        assertThat(osaamisenArviointiList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllOsaamisenArviointis() {
        // Initialize the database
        osaamisenArviointiRepository.saveAndFlush(osaamisenArviointi)

        // Get all the osaamisenArviointiList
        restOsaamisenArviointiMockMvc.perform(get("/api/osaamisen-arviointis?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(osaamisenArviointi.id?.toInt())))
            .andExpect(jsonPath("$.[*].tunnus").value(hasItem(DEFAULT_TUNNUS)))
            .andExpect(jsonPath("$.[*].osasto").value(hasItem(DEFAULT_OSASTO)))
            .andExpect(jsonPath("$.[*].alkamispaiva").value(hasItem(DEFAULT_ALKAMISPAIVA.toString())))
            .andExpect(jsonPath("$.[*].paattymispaiva").value(hasItem(DEFAULT_PAATTYMISPAIVA.toString())))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getOsaamisenArviointi() {
        // Initialize the database
        osaamisenArviointiRepository.saveAndFlush(osaamisenArviointi)

        val id = osaamisenArviointi.id
        assertNotNull(id)

        // Get the osaamisenArviointi
        restOsaamisenArviointiMockMvc.perform(get("/api/osaamisen-arviointis/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(osaamisenArviointi.id?.toInt()))
            .andExpect(jsonPath("$.tunnus").value(DEFAULT_TUNNUS))
            .andExpect(jsonPath("$.osasto").value(DEFAULT_OSASTO))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingOsaamisenArviointi() {
        // Get the osaamisenArviointi
        restOsaamisenArviointiMockMvc.perform(get("/api/osaamisen-arviointis/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateOsaamisenArviointi() {
        // Initialize the database
        osaamisenArviointiRepository.saveAndFlush(osaamisenArviointi)

        val databaseSizeBeforeUpdate = osaamisenArviointiRepository.findAll().size

        // Update the osaamisenArviointi
        val id = osaamisenArviointi.id
        assertNotNull(id)
        val updatedOsaamisenArviointi = osaamisenArviointiRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedOsaamisenArviointi are not directly saved in db
        em.detach(updatedOsaamisenArviointi)
        updatedOsaamisenArviointi.tunnus = UPDATED_TUNNUS
        updatedOsaamisenArviointi.osasto = UPDATED_OSASTO
        updatedOsaamisenArviointi.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedOsaamisenArviointi.paattymispaiva = UPDATED_PAATTYMISPAIVA
        val osaamisenArviointiDTO = osaamisenArviointiMapper.toDto(updatedOsaamisenArviointi)

        restOsaamisenArviointiMockMvc.perform(
            put("/api/osaamisen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisenArviointiDTO))
        ).andExpect(status().isOk)

        // Validate the OsaamisenArviointi in the database
        val osaamisenArviointiList = osaamisenArviointiRepository.findAll()
        assertThat(osaamisenArviointiList).hasSize(databaseSizeBeforeUpdate)
        val testOsaamisenArviointi = osaamisenArviointiList[osaamisenArviointiList.size - 1]
        assertThat(testOsaamisenArviointi.tunnus).isEqualTo(UPDATED_TUNNUS)
        assertThat(testOsaamisenArviointi.osasto).isEqualTo(UPDATED_OSASTO)
        assertThat(testOsaamisenArviointi.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testOsaamisenArviointi.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
    }

    @Test
    @Transactional
    fun updateNonExistingOsaamisenArviointi() {
        val databaseSizeBeforeUpdate = osaamisenArviointiRepository.findAll().size

        // Create the OsaamisenArviointi
        val osaamisenArviointiDTO = osaamisenArviointiMapper.toDto(osaamisenArviointi)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOsaamisenArviointiMockMvc.perform(
            put("/api/osaamisen-arviointis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osaamisenArviointiDTO))
        ).andExpect(status().isBadRequest)

        // Validate the OsaamisenArviointi in the database
        val osaamisenArviointiList = osaamisenArviointiRepository.findAll()
        assertThat(osaamisenArviointiList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteOsaamisenArviointi() {
        // Initialize the database
        osaamisenArviointiRepository.saveAndFlush(osaamisenArviointi)

        val databaseSizeBeforeDelete = osaamisenArviointiRepository.findAll().size

        // Delete the osaamisenArviointi
        restOsaamisenArviointiMockMvc.perform(
            delete("/api/osaamisen-arviointis/{id}", osaamisenArviointi.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val osaamisenArviointiList = osaamisenArviointiRepository.findAll()
        assertThat(osaamisenArviointiList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_TUNNUS = "AAAAAAAAAA"
        private const val UPDATED_TUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_OSASTO = "AAAAAAAAAA"
        private const val UPDATED_OSASTO = "BBBBBBBBBB"

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): OsaamisenArviointi {
            val osaamisenArviointi = OsaamisenArviointi(
                tunnus = DEFAULT_TUNNUS,
                osasto = DEFAULT_OSASTO,
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA
            )

            return osaamisenArviointi
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): OsaamisenArviointi {
            val osaamisenArviointi = OsaamisenArviointi(
                tunnus = UPDATED_TUNNUS,
                osasto = UPDATED_OSASTO,
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA
            )

            return osaamisenArviointi
        }
    }
}
