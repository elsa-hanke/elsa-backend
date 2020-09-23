package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Tyoskentelypaikka
import fi.oulu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.oulu.elsa.repository.TyoskentelypaikkaRepository
import fi.oulu.elsa.service.TyoskentelypaikkaService
import fi.oulu.elsa.service.mapper.TyoskentelypaikkaMapper
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
 * Integration tests for the [TyoskentelypaikkaResource] REST controller.
 *
 * @see TyoskentelypaikkaResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class TyoskentelypaikkaResourceIT {

    @Autowired
    private lateinit var tyoskentelypaikkaRepository: TyoskentelypaikkaRepository

    @Autowired
    private lateinit var tyoskentelypaikkaMapper: TyoskentelypaikkaMapper

    @Autowired
    private lateinit var tyoskentelypaikkaService: TyoskentelypaikkaService

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

    private lateinit var restTyoskentelypaikkaMockMvc: MockMvc

    private lateinit var tyoskentelypaikka: Tyoskentelypaikka

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val tyoskentelypaikkaResource = TyoskentelypaikkaResource(tyoskentelypaikkaService)
        this.restTyoskentelypaikkaMockMvc = MockMvcBuilders.standaloneSetup(tyoskentelypaikkaResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        tyoskentelypaikka = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createTyoskentelypaikka() {
        val databaseSizeBeforeCreate = tyoskentelypaikkaRepository.findAll().size

        // Create the Tyoskentelypaikka
        val tyoskentelypaikkaDTO = tyoskentelypaikkaMapper.toDto(tyoskentelypaikka)
        restTyoskentelypaikkaMockMvc.perform(
            post("/api/tyoskentelypaikkas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelypaikkaDTO))
        ).andExpect(status().isCreated)

        // Validate the Tyoskentelypaikka in the database
        val tyoskentelypaikkaList = tyoskentelypaikkaRepository.findAll()
        assertThat(tyoskentelypaikkaList).hasSize(databaseSizeBeforeCreate + 1)
        val testTyoskentelypaikka = tyoskentelypaikkaList[tyoskentelypaikkaList.size - 1]
        assertThat(testTyoskentelypaikka.nimi).isEqualTo(DEFAULT_NIMI)
        assertThat(testTyoskentelypaikka.tyyppi).isEqualTo(DEFAULT_TYYPPI)
    }

    @Test
    @Transactional
    fun createTyoskentelypaikkaWithExistingId() {
        val databaseSizeBeforeCreate = tyoskentelypaikkaRepository.findAll().size

        // Create the Tyoskentelypaikka with an existing ID
        tyoskentelypaikka.id = 1L
        val tyoskentelypaikkaDTO = tyoskentelypaikkaMapper.toDto(tyoskentelypaikka)

        // An entity with an existing ID cannot be created, so this API call must fail
        restTyoskentelypaikkaMockMvc.perform(
            post("/api/tyoskentelypaikkas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelypaikkaDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Tyoskentelypaikka in the database
        val tyoskentelypaikkaList = tyoskentelypaikkaRepository.findAll()
        assertThat(tyoskentelypaikkaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllTyoskentelypaikkas() {
        // Initialize the database
        tyoskentelypaikkaRepository.saveAndFlush(tyoskentelypaikka)

        // Get all the tyoskentelypaikkaList
        restTyoskentelypaikkaMockMvc.perform(get("/api/tyoskentelypaikkas?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tyoskentelypaikka.id?.toInt())))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
            .andExpect(jsonPath("$.[*].tyyppi").value(hasItem(DEFAULT_TYYPPI.toString())))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getTyoskentelypaikka() {
        // Initialize the database
        tyoskentelypaikkaRepository.saveAndFlush(tyoskentelypaikka)

        val id = tyoskentelypaikka.id
        assertNotNull(id)

        // Get the tyoskentelypaikka
        restTyoskentelypaikkaMockMvc.perform(get("/api/tyoskentelypaikkas/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tyoskentelypaikka.id?.toInt()))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
            .andExpect(jsonPath("$.tyyppi").value(DEFAULT_TYYPPI.toString()))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingTyoskentelypaikka() {
        // Get the tyoskentelypaikka
        restTyoskentelypaikkaMockMvc.perform(get("/api/tyoskentelypaikkas/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateTyoskentelypaikka() {
        // Initialize the database
        tyoskentelypaikkaRepository.saveAndFlush(tyoskentelypaikka)

        val databaseSizeBeforeUpdate = tyoskentelypaikkaRepository.findAll().size

        // Update the tyoskentelypaikka
        val id = tyoskentelypaikka.id
        assertNotNull(id)
        val updatedTyoskentelypaikka = tyoskentelypaikkaRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedTyoskentelypaikka are not directly saved in db
        em.detach(updatedTyoskentelypaikka)
        updatedTyoskentelypaikka.nimi = UPDATED_NIMI
        updatedTyoskentelypaikka.tyyppi = UPDATED_TYYPPI
        val tyoskentelypaikkaDTO = tyoskentelypaikkaMapper.toDto(updatedTyoskentelypaikka)

        restTyoskentelypaikkaMockMvc.perform(
            put("/api/tyoskentelypaikkas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelypaikkaDTO))
        ).andExpect(status().isOk)

        // Validate the Tyoskentelypaikka in the database
        val tyoskentelypaikkaList = tyoskentelypaikkaRepository.findAll()
        assertThat(tyoskentelypaikkaList).hasSize(databaseSizeBeforeUpdate)
        val testTyoskentelypaikka = tyoskentelypaikkaList[tyoskentelypaikkaList.size - 1]
        assertThat(testTyoskentelypaikka.nimi).isEqualTo(UPDATED_NIMI)
        assertThat(testTyoskentelypaikka.tyyppi).isEqualTo(UPDATED_TYYPPI)
    }

    @Test
    @Transactional
    fun updateNonExistingTyoskentelypaikka() {
        val databaseSizeBeforeUpdate = tyoskentelypaikkaRepository.findAll().size

        // Create the Tyoskentelypaikka
        val tyoskentelypaikkaDTO = tyoskentelypaikkaMapper.toDto(tyoskentelypaikka)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTyoskentelypaikkaMockMvc.perform(
            put("/api/tyoskentelypaikkas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelypaikkaDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Tyoskentelypaikka in the database
        val tyoskentelypaikkaList = tyoskentelypaikkaRepository.findAll()
        assertThat(tyoskentelypaikkaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteTyoskentelypaikka() {
        // Initialize the database
        tyoskentelypaikkaRepository.saveAndFlush(tyoskentelypaikka)

        val databaseSizeBeforeDelete = tyoskentelypaikkaRepository.findAll().size

        // Delete the tyoskentelypaikka
        restTyoskentelypaikkaMockMvc.perform(
            delete("/api/tyoskentelypaikkas/{id}", tyoskentelypaikka.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val tyoskentelypaikkaList = tyoskentelypaikkaRepository.findAll()
        assertThat(tyoskentelypaikkaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        private val UPDATED_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.SAIRAALA

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = DEFAULT_NIMI,
                tyyppi = DEFAULT_TYYPPI
            )

            return tyoskentelypaikka
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = UPDATED_NIMI,
                tyyppi = UPDATED_TYYPPI
            )

            return tyoskentelypaikka
        }
    }
}
