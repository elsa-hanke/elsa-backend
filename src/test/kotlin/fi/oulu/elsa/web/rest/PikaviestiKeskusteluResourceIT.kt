package fi.oulu.elsa.web.rest

import fi.oulu.elsa.ElsaBackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.PikaviestiKeskustelu
import fi.oulu.elsa.repository.PikaviestiKeskusteluRepository
import fi.oulu.elsa.service.PikaviestiKeskusteluService
import fi.oulu.elsa.service.mapper.PikaviestiKeskusteluMapper
import fi.oulu.elsa.web.rest.errors.ExceptionTranslator
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
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
 * Integration tests for the [PikaviestiKeskusteluResource] REST controller.
 *
 * @see PikaviestiKeskusteluResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
@Extensions(
    ExtendWith(MockitoExtension::class)
)
class PikaviestiKeskusteluResourceIT {

    @Autowired
    private lateinit var pikaviestiKeskusteluRepository: PikaviestiKeskusteluRepository

    @Mock
    private lateinit var pikaviestiKeskusteluRepositoryMock: PikaviestiKeskusteluRepository

    @Autowired
    private lateinit var pikaviestiKeskusteluMapper: PikaviestiKeskusteluMapper

    @Mock
    private lateinit var pikaviestiKeskusteluServiceMock: PikaviestiKeskusteluService

    @Autowired
    private lateinit var pikaviestiKeskusteluService: PikaviestiKeskusteluService

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

    private lateinit var restPikaviestiKeskusteluMockMvc: MockMvc

    private lateinit var pikaviestiKeskustelu: PikaviestiKeskustelu

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val pikaviestiKeskusteluResource = PikaviestiKeskusteluResource(pikaviestiKeskusteluService)
        this.restPikaviestiKeskusteluMockMvc = MockMvcBuilders.standaloneSetup(pikaviestiKeskusteluResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        pikaviestiKeskustelu = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createPikaviestiKeskustelu() {
        val databaseSizeBeforeCreate = pikaviestiKeskusteluRepository.findAll().size

        // Create the PikaviestiKeskustelu
        val pikaviestiKeskusteluDTO = pikaviestiKeskusteluMapper.toDto(pikaviestiKeskustelu)
        restPikaviestiKeskusteluMockMvc.perform(
            post("/api/pikaviesti-keskustelus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiKeskusteluDTO))
        ).andExpect(status().isCreated)

        // Validate the PikaviestiKeskustelu in the database
        val pikaviestiKeskusteluList = pikaviestiKeskusteluRepository.findAll()
        assertThat(pikaviestiKeskusteluList).hasSize(databaseSizeBeforeCreate + 1)
        val testPikaviestiKeskustelu = pikaviestiKeskusteluList[pikaviestiKeskusteluList.size - 1]
        assertThat(testPikaviestiKeskustelu.aihe).isEqualTo(DEFAULT_AIHE)
    }

    @Test
    @Transactional
    fun createPikaviestiKeskusteluWithExistingId() {
        val databaseSizeBeforeCreate = pikaviestiKeskusteluRepository.findAll().size

        // Create the PikaviestiKeskustelu with an existing ID
        pikaviestiKeskustelu.id = 1L
        val pikaviestiKeskusteluDTO = pikaviestiKeskusteluMapper.toDto(pikaviestiKeskustelu)

        // An entity with an existing ID cannot be created, so this API call must fail
        restPikaviestiKeskusteluMockMvc.perform(
            post("/api/pikaviesti-keskustelus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiKeskusteluDTO))
        ).andExpect(status().isBadRequest)

        // Validate the PikaviestiKeskustelu in the database
        val pikaviestiKeskusteluList = pikaviestiKeskusteluRepository.findAll()
        assertThat(pikaviestiKeskusteluList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPikaviestiKeskustelus() {
        // Initialize the database
        pikaviestiKeskusteluRepository.saveAndFlush(pikaviestiKeskustelu)

        // Get all the pikaviestiKeskusteluList
        restPikaviestiKeskusteluMockMvc.perform(get("/api/pikaviesti-keskustelus?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pikaviestiKeskustelu.id?.toInt())))
            .andExpect(jsonPath("$.[*].aihe").value(hasItem(DEFAULT_AIHE)))
    }

    @Suppress("unchecked")
    @Throws(Exception::class)
    fun getAllPikaviestiKeskustelusWithEagerRelationshipsIsEnabled() {
        val pikaviestiKeskusteluResource = PikaviestiKeskusteluResource(pikaviestiKeskusteluServiceMock)
        `when`(pikaviestiKeskusteluServiceMock.findAllWithEagerRelationships(any())).thenReturn(PageImpl(mutableListOf()))

        val restPikaviestiKeskusteluMockMvc = MockMvcBuilders.standaloneSetup(pikaviestiKeskusteluResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build()

        restPikaviestiKeskusteluMockMvc.perform(get("/api/pikaviesti-keskustelus?eagerload=true"))
            .andExpect(status().isOk)

        verify(pikaviestiKeskusteluServiceMock, times(1)).findAllWithEagerRelationships(any())
    }

    @Suppress("unchecked")
    @Throws(Exception::class)
    fun getAllPikaviestiKeskustelusWithEagerRelationshipsIsNotEnabled() {
        val pikaviestiKeskusteluResource = PikaviestiKeskusteluResource(pikaviestiKeskusteluServiceMock)
        `when`(pikaviestiKeskusteluServiceMock.findAllWithEagerRelationships(any())).thenReturn(PageImpl(mutableListOf()))

        val restPikaviestiKeskusteluMockMvc = MockMvcBuilders.standaloneSetup(pikaviestiKeskusteluResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build()

        restPikaviestiKeskusteluMockMvc.perform(get("/api/pikaviesti-keskustelus?eagerload=true"))
            .andExpect(status().isOk)

        verify(pikaviestiKeskusteluServiceMock, times(1)).findAllWithEagerRelationships(any())
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getPikaviestiKeskustelu() {
        // Initialize the database
        pikaviestiKeskusteluRepository.saveAndFlush(pikaviestiKeskustelu)

        val id = pikaviestiKeskustelu.id
        assertNotNull(id)

        // Get the pikaviestiKeskustelu
        restPikaviestiKeskusteluMockMvc.perform(get("/api/pikaviesti-keskustelus/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pikaviestiKeskustelu.id?.toInt()))
            .andExpect(jsonPath("$.aihe").value(DEFAULT_AIHE))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingPikaviestiKeskustelu() {
        // Get the pikaviestiKeskustelu
        restPikaviestiKeskusteluMockMvc.perform(get("/api/pikaviesti-keskustelus/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updatePikaviestiKeskustelu() {
        // Initialize the database
        pikaviestiKeskusteluRepository.saveAndFlush(pikaviestiKeskustelu)

        val databaseSizeBeforeUpdate = pikaviestiKeskusteluRepository.findAll().size

        // Update the pikaviestiKeskustelu
        val id = pikaviestiKeskustelu.id
        assertNotNull(id)
        val updatedPikaviestiKeskustelu = pikaviestiKeskusteluRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedPikaviestiKeskustelu are not directly saved in db
        em.detach(updatedPikaviestiKeskustelu)
        updatedPikaviestiKeskustelu.aihe = UPDATED_AIHE
        val pikaviestiKeskusteluDTO = pikaviestiKeskusteluMapper.toDto(updatedPikaviestiKeskustelu)

        restPikaviestiKeskusteluMockMvc.perform(
            put("/api/pikaviesti-keskustelus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiKeskusteluDTO))
        ).andExpect(status().isOk)

        // Validate the PikaviestiKeskustelu in the database
        val pikaviestiKeskusteluList = pikaviestiKeskusteluRepository.findAll()
        assertThat(pikaviestiKeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testPikaviestiKeskustelu = pikaviestiKeskusteluList[pikaviestiKeskusteluList.size - 1]
        assertThat(testPikaviestiKeskustelu.aihe).isEqualTo(UPDATED_AIHE)
    }

    @Test
    @Transactional
    fun updateNonExistingPikaviestiKeskustelu() {
        val databaseSizeBeforeUpdate = pikaviestiKeskusteluRepository.findAll().size

        // Create the PikaviestiKeskustelu
        val pikaviestiKeskusteluDTO = pikaviestiKeskusteluMapper.toDto(pikaviestiKeskustelu)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPikaviestiKeskusteluMockMvc.perform(
            put("/api/pikaviesti-keskustelus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(pikaviestiKeskusteluDTO))
        ).andExpect(status().isBadRequest)

        // Validate the PikaviestiKeskustelu in the database
        val pikaviestiKeskusteluList = pikaviestiKeskusteluRepository.findAll()
        assertThat(pikaviestiKeskusteluList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deletePikaviestiKeskustelu() {
        // Initialize the database
        pikaviestiKeskusteluRepository.saveAndFlush(pikaviestiKeskustelu)

        val databaseSizeBeforeDelete = pikaviestiKeskusteluRepository.findAll().size

        // Delete the pikaviestiKeskustelu
        restPikaviestiKeskusteluMockMvc.perform(
            delete("/api/pikaviesti-keskustelus/{id}", pikaviestiKeskustelu.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val pikaviestiKeskusteluList = pikaviestiKeskusteluRepository.findAll()
        assertThat(pikaviestiKeskusteluList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_AIHE = "AAAAAAAAAA"
        private const val UPDATED_AIHE = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): PikaviestiKeskustelu {
            val pikaviestiKeskustelu = PikaviestiKeskustelu(
                aihe = DEFAULT_AIHE
            )

            return pikaviestiKeskustelu
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): PikaviestiKeskustelu {
            val pikaviestiKeskustelu = PikaviestiKeskustelu(
                aihe = UPDATED_AIHE
            )

            return pikaviestiKeskustelu
        }
    }
}
