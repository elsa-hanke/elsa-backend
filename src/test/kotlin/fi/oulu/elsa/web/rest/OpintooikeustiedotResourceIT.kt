package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Opintooikeustiedot
import fi.oulu.elsa.repository.OpintooikeustiedotRepository
import fi.oulu.elsa.service.OpintooikeustiedotService
import fi.oulu.elsa.service.mapper.OpintooikeustiedotMapper
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
 * Integration tests for the [OpintooikeustiedotResource] REST controller.
 *
 * @see OpintooikeustiedotResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class OpintooikeustiedotResourceIT {

    @Autowired
    private lateinit var opintooikeustiedotRepository: OpintooikeustiedotRepository

    @Autowired
    private lateinit var opintooikeustiedotMapper: OpintooikeustiedotMapper

    @Autowired
    private lateinit var opintooikeustiedotService: OpintooikeustiedotService

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

    private lateinit var restOpintooikeustiedotMockMvc: MockMvc

    private lateinit var opintooikeustiedot: Opintooikeustiedot

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val opintooikeustiedotResource = OpintooikeustiedotResource(opintooikeustiedotService)
        this.restOpintooikeustiedotMockMvc = MockMvcBuilders.standaloneSetup(opintooikeustiedotResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        opintooikeustiedot = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createOpintooikeustiedot() {
        val databaseSizeBeforeCreate = opintooikeustiedotRepository.findAll().size

        // Create the Opintooikeustiedot
        val opintooikeustiedotDTO = opintooikeustiedotMapper.toDto(opintooikeustiedot)
        restOpintooikeustiedotMockMvc.perform(
            post("/api/opintooikeustiedots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintooikeustiedotDTO))
        ).andExpect(status().isCreated)

        // Validate the Opintooikeustiedot in the database
        val opintooikeustiedotList = opintooikeustiedotRepository.findAll()
        assertThat(opintooikeustiedotList).hasSize(databaseSizeBeforeCreate + 1)
        val testOpintooikeustiedot = opintooikeustiedotList[opintooikeustiedotList.size - 1]
        assertThat(testOpintooikeustiedot.voimassaoloAlkaa).isEqualTo(DEFAULT_VOIMASSAOLO_ALKAA)
        assertThat(testOpintooikeustiedot.voimassaoloPaattyy).isEqualTo(DEFAULT_VOIMASSAOLO_PAATTYY)
        assertThat(testOpintooikeustiedot.erikoisala).isEqualTo(DEFAULT_ERIKOISALA)
    }

    @Test
    @Transactional
    fun createOpintooikeustiedotWithExistingId() {
        val databaseSizeBeforeCreate = opintooikeustiedotRepository.findAll().size

        // Create the Opintooikeustiedot with an existing ID
        opintooikeustiedot.id = 1L
        val opintooikeustiedotDTO = opintooikeustiedotMapper.toDto(opintooikeustiedot)

        // An entity with an existing ID cannot be created, so this API call must fail
        restOpintooikeustiedotMockMvc.perform(
            post("/api/opintooikeustiedots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintooikeustiedotDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Opintooikeustiedot in the database
        val opintooikeustiedotList = opintooikeustiedotRepository.findAll()
        assertThat(opintooikeustiedotList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllOpintooikeustiedots() {
        // Initialize the database
        opintooikeustiedotRepository.saveAndFlush(opintooikeustiedot)

        // Get all the opintooikeustiedotList
        restOpintooikeustiedotMockMvc.perform(get("/api/opintooikeustiedots?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(opintooikeustiedot.id?.toInt())))
            .andExpect(jsonPath("$.[*].voimassaoloAlkaa").value(hasItem(DEFAULT_VOIMASSAOLO_ALKAA.toString())))
            .andExpect(jsonPath("$.[*].voimassaoloPaattyy").value(hasItem(DEFAULT_VOIMASSAOLO_PAATTYY.toString())))
            .andExpect(jsonPath("$.[*].erikoisala").value(hasItem(DEFAULT_ERIKOISALA)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getOpintooikeustiedot() {
        // Initialize the database
        opintooikeustiedotRepository.saveAndFlush(opintooikeustiedot)

        val id = opintooikeustiedot.id
        assertNotNull(id)

        // Get the opintooikeustiedot
        restOpintooikeustiedotMockMvc.perform(get("/api/opintooikeustiedots/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(opintooikeustiedot.id?.toInt()))
            .andExpect(jsonPath("$.voimassaoloAlkaa").value(DEFAULT_VOIMASSAOLO_ALKAA.toString()))
            .andExpect(jsonPath("$.voimassaoloPaattyy").value(DEFAULT_VOIMASSAOLO_PAATTYY.toString()))
            .andExpect(jsonPath("$.erikoisala").value(DEFAULT_ERIKOISALA))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingOpintooikeustiedot() {
        // Get the opintooikeustiedot
        restOpintooikeustiedotMockMvc.perform(get("/api/opintooikeustiedots/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateOpintooikeustiedot() {
        // Initialize the database
        opintooikeustiedotRepository.saveAndFlush(opintooikeustiedot)

        val databaseSizeBeforeUpdate = opintooikeustiedotRepository.findAll().size

        // Update the opintooikeustiedot
        val id = opintooikeustiedot.id
        assertNotNull(id)
        val updatedOpintooikeustiedot = opintooikeustiedotRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedOpintooikeustiedot are not directly saved in db
        em.detach(updatedOpintooikeustiedot)
        updatedOpintooikeustiedot.voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA
        updatedOpintooikeustiedot.voimassaoloPaattyy = UPDATED_VOIMASSAOLO_PAATTYY
        updatedOpintooikeustiedot.erikoisala = UPDATED_ERIKOISALA
        val opintooikeustiedotDTO = opintooikeustiedotMapper.toDto(updatedOpintooikeustiedot)

        restOpintooikeustiedotMockMvc.perform(
            put("/api/opintooikeustiedots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintooikeustiedotDTO))
        ).andExpect(status().isOk)

        // Validate the Opintooikeustiedot in the database
        val opintooikeustiedotList = opintooikeustiedotRepository.findAll()
        assertThat(opintooikeustiedotList).hasSize(databaseSizeBeforeUpdate)
        val testOpintooikeustiedot = opintooikeustiedotList[opintooikeustiedotList.size - 1]
        assertThat(testOpintooikeustiedot.voimassaoloAlkaa).isEqualTo(UPDATED_VOIMASSAOLO_ALKAA)
        assertThat(testOpintooikeustiedot.voimassaoloPaattyy).isEqualTo(UPDATED_VOIMASSAOLO_PAATTYY)
        assertThat(testOpintooikeustiedot.erikoisala).isEqualTo(UPDATED_ERIKOISALA)
    }

    @Test
    @Transactional
    fun updateNonExistingOpintooikeustiedot() {
        val databaseSizeBeforeUpdate = opintooikeustiedotRepository.findAll().size

        // Create the Opintooikeustiedot
        val opintooikeustiedotDTO = opintooikeustiedotMapper.toDto(opintooikeustiedot)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpintooikeustiedotMockMvc.perform(
            put("/api/opintooikeustiedots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintooikeustiedotDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Opintooikeustiedot in the database
        val opintooikeustiedotList = opintooikeustiedotRepository.findAll()
        assertThat(opintooikeustiedotList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteOpintooikeustiedot() {
        // Initialize the database
        opintooikeustiedotRepository.saveAndFlush(opintooikeustiedot)

        val databaseSizeBeforeDelete = opintooikeustiedotRepository.findAll().size

        // Delete the opintooikeustiedot
        restOpintooikeustiedotMockMvc.perform(
            delete("/api/opintooikeustiedots/{id}", opintooikeustiedot.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val opintooikeustiedotList = opintooikeustiedotRepository.findAll()
        assertThat(opintooikeustiedotList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_ERIKOISALA = "AAAAAAAAAA"
        private const val UPDATED_ERIKOISALA = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Opintooikeustiedot {
            val opintooikeustiedot = Opintooikeustiedot(
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = DEFAULT_VOIMASSAOLO_PAATTYY,
                erikoisala = DEFAULT_ERIKOISALA
            )

            return opintooikeustiedot
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Opintooikeustiedot {
            val opintooikeustiedot = Opintooikeustiedot(
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = UPDATED_VOIMASSAOLO_PAATTYY,
                erikoisala = UPDATED_ERIKOISALA
            )

            return opintooikeustiedot
        }
    }
}
