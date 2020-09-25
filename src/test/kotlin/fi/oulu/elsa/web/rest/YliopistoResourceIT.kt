package fi.oulu.elsa.web.rest

import fi.oulu.elsa.ElsaBackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Yliopisto
import fi.oulu.elsa.repository.YliopistoRepository
import fi.oulu.elsa.service.YliopistoService
import fi.oulu.elsa.service.mapper.YliopistoMapper
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
 * Integration tests for the [YliopistoResource] REST controller.
 *
 * @see YliopistoResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
@Extensions(
    ExtendWith(MockitoExtension::class)
)
class YliopistoResourceIT {

    @Autowired
    private lateinit var yliopistoRepository: YliopistoRepository

    @Mock
    private lateinit var yliopistoRepositoryMock: YliopistoRepository

    @Autowired
    private lateinit var yliopistoMapper: YliopistoMapper

    @Mock
    private lateinit var yliopistoServiceMock: YliopistoService

    @Autowired
    private lateinit var yliopistoService: YliopistoService

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

    private lateinit var restYliopistoMockMvc: MockMvc

    private lateinit var yliopisto: Yliopisto

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val yliopistoResource = YliopistoResource(yliopistoService)
        this.restYliopistoMockMvc = MockMvcBuilders.standaloneSetup(yliopistoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        yliopisto = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createYliopisto() {
        val databaseSizeBeforeCreate = yliopistoRepository.findAll().size

        // Create the Yliopisto
        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        restYliopistoMockMvc.perform(
            post("/api/yliopistos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(yliopistoDTO))
        ).andExpect(status().isCreated)

        // Validate the Yliopisto in the database
        val yliopistoList = yliopistoRepository.findAll()
        assertThat(yliopistoList).hasSize(databaseSizeBeforeCreate + 1)
        val testYliopisto = yliopistoList[yliopistoList.size - 1]
        assertThat(testYliopisto.nimi).isEqualTo(DEFAULT_NIMI)
    }

    @Test
    @Transactional
    fun createYliopistoWithExistingId() {
        val databaseSizeBeforeCreate = yliopistoRepository.findAll().size

        // Create the Yliopisto with an existing ID
        yliopisto.id = 1L
        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)

        // An entity with an existing ID cannot be created, so this API call must fail
        restYliopistoMockMvc.perform(
            post("/api/yliopistos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(yliopistoDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Yliopisto in the database
        val yliopistoList = yliopistoRepository.findAll()
        assertThat(yliopistoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllYliopistos() {
        // Initialize the database
        yliopistoRepository.saveAndFlush(yliopisto)

        // Get all the yliopistoList
        restYliopistoMockMvc.perform(get("/api/yliopistos?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(yliopisto.id?.toInt())))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
    }

    @Suppress("unchecked")
    @Throws(Exception::class)
    fun getAllYliopistosWithEagerRelationshipsIsEnabled() {
        val yliopistoResource = YliopistoResource(yliopistoServiceMock)
        `when`(yliopistoServiceMock.findAllWithEagerRelationships(any())).thenReturn(PageImpl(mutableListOf()))

        val restYliopistoMockMvc = MockMvcBuilders.standaloneSetup(yliopistoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build()

        restYliopistoMockMvc.perform(get("/api/yliopistos?eagerload=true"))
            .andExpect(status().isOk)

        verify(yliopistoServiceMock, times(1)).findAllWithEagerRelationships(any())
    }

    @Suppress("unchecked")
    @Throws(Exception::class)
    fun getAllYliopistosWithEagerRelationshipsIsNotEnabled() {
        val yliopistoResource = YliopistoResource(yliopistoServiceMock)
        `when`(yliopistoServiceMock.findAllWithEagerRelationships(any())).thenReturn(PageImpl(mutableListOf()))

        val restYliopistoMockMvc = MockMvcBuilders.standaloneSetup(yliopistoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build()

        restYliopistoMockMvc.perform(get("/api/yliopistos?eagerload=true"))
            .andExpect(status().isOk)

        verify(yliopistoServiceMock, times(1)).findAllWithEagerRelationships(any())
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getYliopisto() {
        // Initialize the database
        yliopistoRepository.saveAndFlush(yliopisto)

        val id = yliopisto.id
        assertNotNull(id)

        // Get the yliopisto
        restYliopistoMockMvc.perform(get("/api/yliopistos/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(yliopisto.id?.toInt()))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingYliopisto() {
        // Get the yliopisto
        restYliopistoMockMvc.perform(get("/api/yliopistos/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateYliopisto() {
        // Initialize the database
        yliopistoRepository.saveAndFlush(yliopisto)

        val databaseSizeBeforeUpdate = yliopistoRepository.findAll().size

        // Update the yliopisto
        val id = yliopisto.id
        assertNotNull(id)
        val updatedYliopisto = yliopistoRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedYliopisto are not directly saved in db
        em.detach(updatedYliopisto)
        updatedYliopisto.nimi = UPDATED_NIMI
        val yliopistoDTO = yliopistoMapper.toDto(updatedYliopisto)

        restYliopistoMockMvc.perform(
            put("/api/yliopistos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(yliopistoDTO))
        ).andExpect(status().isOk)

        // Validate the Yliopisto in the database
        val yliopistoList = yliopistoRepository.findAll()
        assertThat(yliopistoList).hasSize(databaseSizeBeforeUpdate)
        val testYliopisto = yliopistoList[yliopistoList.size - 1]
        assertThat(testYliopisto.nimi).isEqualTo(UPDATED_NIMI)
    }

    @Test
    @Transactional
    fun updateNonExistingYliopisto() {
        val databaseSizeBeforeUpdate = yliopistoRepository.findAll().size

        // Create the Yliopisto
        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restYliopistoMockMvc.perform(
            put("/api/yliopistos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(yliopistoDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Yliopisto in the database
        val yliopistoList = yliopistoRepository.findAll()
        assertThat(yliopistoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteYliopisto() {
        // Initialize the database
        yliopistoRepository.saveAndFlush(yliopisto)

        val databaseSizeBeforeDelete = yliopistoRepository.findAll().size

        // Delete the yliopisto
        restYliopistoMockMvc.perform(
            delete("/api/yliopistos/{id}", yliopisto.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val yliopistoList = yliopistoRepository.findAll()
        assertThat(yliopistoList).hasSize(databaseSizeBeforeDelete - 1)
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
        fun createEntity(em: EntityManager): Yliopisto {
            val yliopisto = Yliopisto(
                nimi = DEFAULT_NIMI
            )

            return yliopisto
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Yliopisto {
            val yliopisto = Yliopisto(
                nimi = UPDATED_NIMI
            )

            return yliopisto
        }
    }
}
