package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Koejakso
import fi.elsapalvelu.elsa.repository.KoejaksoRepository
import fi.elsapalvelu.elsa.service.KoejaksoService
import fi.elsapalvelu.elsa.service.mapper.KoejaksoMapper
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
 * Integration tests for the [KoejaksoResource] REST controller.
 *
 * @see KoejaksoResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class KoejaksoResourceIT {

    @Autowired
    private lateinit var koejaksoRepository: KoejaksoRepository

    @Autowired
    private lateinit var koejaksoMapper: KoejaksoMapper

    @Autowired
    private lateinit var koejaksoService: KoejaksoService

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

    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejakso: Koejakso

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val koejaksoResource = KoejaksoResource(koejaksoService)
        this.restKoejaksoMockMvc = MockMvcBuilders.standaloneSetup(koejaksoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        koejakso = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createKoejakso() {
        val databaseSizeBeforeCreate = koejaksoRepository.findAll().size

        // Create the Koejakso
        val koejaksoDTO = koejaksoMapper.toDto(koejakso)
        restKoejaksoMockMvc.perform(
            post("/api/koejaksos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksoDTO))
        ).andExpect(status().isCreated)

        // Validate the Koejakso in the database
        val koejaksoList = koejaksoRepository.findAll()
        assertThat(koejaksoList).hasSize(databaseSizeBeforeCreate + 1)
        val testKoejakso = koejaksoList[koejaksoList.size - 1]
        assertThat(testKoejakso.ohjeteksti).isEqualTo(DEFAULT_OHJETEKSTI)
        assertThat(testKoejakso.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testKoejakso.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
    }

    @Test
    @Transactional
    fun createKoejaksoWithExistingId() {
        val databaseSizeBeforeCreate = koejaksoRepository.findAll().size

        // Create the Koejakso with an existing ID
        koejakso.id = 1L
        val koejaksoDTO = koejaksoMapper.toDto(koejakso)

        // An entity with an existing ID cannot be created, so this API call must fail
        restKoejaksoMockMvc.perform(
            post("/api/koejaksos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksoDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Koejakso in the database
        val koejaksoList = koejaksoRepository.findAll()
        assertThat(koejaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllKoejaksos() {
        // Initialize the database
        koejaksoRepository.saveAndFlush(koejakso)

        // Get all the koejaksoList
        restKoejaksoMockMvc.perform(get("/api/koejaksos?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(koejakso.id?.toInt())))
            .andExpect(jsonPath("$.[*].ohjeteksti").value(hasItem(DEFAULT_OHJETEKSTI)))
            .andExpect(jsonPath("$.[*].alkamispaiva").value(hasItem(DEFAULT_ALKAMISPAIVA.toString())))
            .andExpect(jsonPath("$.[*].paattymispaiva").value(hasItem(DEFAULT_PAATTYMISPAIVA.toString())))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getKoejakso() {
        // Initialize the database
        koejaksoRepository.saveAndFlush(koejakso)

        val id = koejakso.id
        assertNotNull(id)

        // Get the koejakso
        restKoejaksoMockMvc.perform(get("/api/koejaksos/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejakso.id?.toInt()))
            .andExpect(jsonPath("$.ohjeteksti").value(DEFAULT_OHJETEKSTI))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingKoejakso() {
        // Get the koejakso
        restKoejaksoMockMvc.perform(get("/api/koejaksos/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateKoejakso() {
        // Initialize the database
        koejaksoRepository.saveAndFlush(koejakso)

        val databaseSizeBeforeUpdate = koejaksoRepository.findAll().size

        // Update the koejakso
        val id = koejakso.id
        assertNotNull(id)
        val updatedKoejakso = koejaksoRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedKoejakso are not directly saved in db
        em.detach(updatedKoejakso)
        updatedKoejakso.ohjeteksti = UPDATED_OHJETEKSTI
        updatedKoejakso.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedKoejakso.paattymispaiva = UPDATED_PAATTYMISPAIVA
        val koejaksoDTO = koejaksoMapper.toDto(updatedKoejakso)

        restKoejaksoMockMvc.perform(
            put("/api/koejaksos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksoDTO))
        ).andExpect(status().isOk)

        // Validate the Koejakso in the database
        val koejaksoList = koejaksoRepository.findAll()
        assertThat(koejaksoList).hasSize(databaseSizeBeforeUpdate)
        val testKoejakso = koejaksoList[koejaksoList.size - 1]
        assertThat(testKoejakso.ohjeteksti).isEqualTo(UPDATED_OHJETEKSTI)
        assertThat(testKoejakso.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testKoejakso.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
    }

    @Test
    @Transactional
    fun updateNonExistingKoejakso() {
        val databaseSizeBeforeUpdate = koejaksoRepository.findAll().size

        // Create the Koejakso
        val koejaksoDTO = koejaksoMapper.toDto(koejakso)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restKoejaksoMockMvc.perform(
            put("/api/koejaksos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksoDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Koejakso in the database
        val koejaksoList = koejaksoRepository.findAll()
        assertThat(koejaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteKoejakso() {
        // Initialize the database
        koejaksoRepository.saveAndFlush(koejakso)

        val databaseSizeBeforeDelete = koejaksoRepository.findAll().size

        // Delete the koejakso
        restKoejaksoMockMvc.perform(
            delete("/api/koejaksos/{id}", koejakso.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val koejaksoList = koejaksoRepository.findAll()
        assertThat(koejaksoList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_OHJETEKSTI = "AAAAAAAAAA"
        private const val UPDATED_OHJETEKSTI = "BBBBBBBBBB"

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
        fun createEntity(em: EntityManager): Koejakso {
            val koejakso = Koejakso(
                ohjeteksti = DEFAULT_OHJETEKSTI,
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA
            )

            return koejakso
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Koejakso {
            val koejakso = Koejakso(
                ohjeteksti = UPDATED_OHJETEKSTI,
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA
            )

            return koejakso
        }
    }
}
