package fi.oulu.elsa.web.rest

import fi.oulu.elsa.ElsaBackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Arviointiosaalue
import fi.oulu.elsa.repository.ArviointiosaalueRepository
import fi.oulu.elsa.service.ArviointiosaalueService
import fi.oulu.elsa.service.mapper.ArviointiosaalueMapper
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
 * Integration tests for the [ArviointiosaalueResource] REST controller.
 *
 * @see ArviointiosaalueResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class ArviointiosaalueResourceIT {

    @Autowired
    private lateinit var arviointiosaalueRepository: ArviointiosaalueRepository

    @Autowired
    private lateinit var arviointiosaalueMapper: ArviointiosaalueMapper

    @Autowired
    private lateinit var arviointiosaalueService: ArviointiosaalueService

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

    private lateinit var restArviointiosaalueMockMvc: MockMvc

    private lateinit var arviointiosaalue: Arviointiosaalue

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val arviointiosaalueResource = ArviointiosaalueResource(arviointiosaalueService)
        this.restArviointiosaalueMockMvc = MockMvcBuilders.standaloneSetup(arviointiosaalueResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        arviointiosaalue = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createArviointiosaalue() {
        val databaseSizeBeforeCreate = arviointiosaalueRepository.findAll().size

        // Create the Arviointiosaalue
        val arviointiosaalueDTO = arviointiosaalueMapper.toDto(arviointiosaalue)
        restArviointiosaalueMockMvc.perform(
            post("/api/arviointiosaalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arviointiosaalueDTO))
        ).andExpect(status().isCreated)

        // Validate the Arviointiosaalue in the database
        val arviointiosaalueList = arviointiosaalueRepository.findAll()
        assertThat(arviointiosaalueList).hasSize(databaseSizeBeforeCreate + 1)
        val testArviointiosaalue = arviointiosaalueList[arviointiosaalueList.size - 1]
        assertThat(testArviointiosaalue.aluetunnus).isEqualTo(DEFAULT_ALUETUNNUS)
        assertThat(testArviointiosaalue.nimi).isEqualTo(DEFAULT_NIMI)
        assertThat(testArviointiosaalue.kuvaus).isEqualTo(DEFAULT_KUVAUS)
        assertThat(testArviointiosaalue.osaamisenRajaarvo).isEqualTo(DEFAULT_OSAAMISEN_RAJAARVO)
        assertThat(testArviointiosaalue.minimivaatimus).isEqualTo(DEFAULT_MINIMIVAATIMUS)
        assertThat(testArviointiosaalue.voimassaoloAlkaa).isEqualTo(DEFAULT_VOIMASSAOLO_ALKAA)
        assertThat(testArviointiosaalue.voimassaoloLoppuu).isEqualTo(DEFAULT_VOIMASSAOLO_LOPPUU)
    }

    @Test
    @Transactional
    fun createArviointiosaalueWithExistingId() {
        val databaseSizeBeforeCreate = arviointiosaalueRepository.findAll().size

        // Create the Arviointiosaalue with an existing ID
        arviointiosaalue.id = 1L
        val arviointiosaalueDTO = arviointiosaalueMapper.toDto(arviointiosaalue)

        // An entity with an existing ID cannot be created, so this API call must fail
        restArviointiosaalueMockMvc.perform(
            post("/api/arviointiosaalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arviointiosaalueDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Arviointiosaalue in the database
        val arviointiosaalueList = arviointiosaalueRepository.findAll()
        assertThat(arviointiosaalueList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllArviointiosaalues() {
        // Initialize the database
        arviointiosaalueRepository.saveAndFlush(arviointiosaalue)

        // Get all the arviointiosaalueList
        restArviointiosaalueMockMvc.perform(get("/api/arviointiosaalues?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(arviointiosaalue.id?.toInt())))
            .andExpect(jsonPath("$.[*].aluetunnus").value(hasItem(DEFAULT_ALUETUNNUS)))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
            .andExpect(jsonPath("$.[*].kuvaus").value(hasItem(DEFAULT_KUVAUS)))
            .andExpect(jsonPath("$.[*].osaamisenRajaarvo").value(hasItem(DEFAULT_OSAAMISEN_RAJAARVO)))
            .andExpect(jsonPath("$.[*].minimivaatimus").value(hasItem(DEFAULT_MINIMIVAATIMUS)))
            .andExpect(jsonPath("$.[*].voimassaoloAlkaa").value(hasItem(DEFAULT_VOIMASSAOLO_ALKAA.toString())))
            .andExpect(jsonPath("$.[*].voimassaoloLoppuu").value(hasItem(DEFAULT_VOIMASSAOLO_LOPPUU.toString())))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getArviointiosaalue() {
        // Initialize the database
        arviointiosaalueRepository.saveAndFlush(arviointiosaalue)

        val id = arviointiosaalue.id
        assertNotNull(id)

        // Get the arviointiosaalue
        restArviointiosaalueMockMvc.perform(get("/api/arviointiosaalues/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(arviointiosaalue.id?.toInt()))
            .andExpect(jsonPath("$.aluetunnus").value(DEFAULT_ALUETUNNUS))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
            .andExpect(jsonPath("$.kuvaus").value(DEFAULT_KUVAUS))
            .andExpect(jsonPath("$.osaamisenRajaarvo").value(DEFAULT_OSAAMISEN_RAJAARVO))
            .andExpect(jsonPath("$.minimivaatimus").value(DEFAULT_MINIMIVAATIMUS))
            .andExpect(jsonPath("$.voimassaoloAlkaa").value(DEFAULT_VOIMASSAOLO_ALKAA.toString()))
            .andExpect(jsonPath("$.voimassaoloLoppuu").value(DEFAULT_VOIMASSAOLO_LOPPUU.toString()))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingArviointiosaalue() {
        // Get the arviointiosaalue
        restArviointiosaalueMockMvc.perform(get("/api/arviointiosaalues/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateArviointiosaalue() {
        // Initialize the database
        arviointiosaalueRepository.saveAndFlush(arviointiosaalue)

        val databaseSizeBeforeUpdate = arviointiosaalueRepository.findAll().size

        // Update the arviointiosaalue
        val id = arviointiosaalue.id
        assertNotNull(id)
        val updatedArviointiosaalue = arviointiosaalueRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedArviointiosaalue are not directly saved in db
        em.detach(updatedArviointiosaalue)
        updatedArviointiosaalue.aluetunnus = UPDATED_ALUETUNNUS
        updatedArviointiosaalue.nimi = UPDATED_NIMI
        updatedArviointiosaalue.kuvaus = UPDATED_KUVAUS
        updatedArviointiosaalue.osaamisenRajaarvo = UPDATED_OSAAMISEN_RAJAARVO
        updatedArviointiosaalue.minimivaatimus = UPDATED_MINIMIVAATIMUS
        updatedArviointiosaalue.voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA
        updatedArviointiosaalue.voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
        val arviointiosaalueDTO = arviointiosaalueMapper.toDto(updatedArviointiosaalue)

        restArviointiosaalueMockMvc.perform(
            put("/api/arviointiosaalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arviointiosaalueDTO))
        ).andExpect(status().isOk)

        // Validate the Arviointiosaalue in the database
        val arviointiosaalueList = arviointiosaalueRepository.findAll()
        assertThat(arviointiosaalueList).hasSize(databaseSizeBeforeUpdate)
        val testArviointiosaalue = arviointiosaalueList[arviointiosaalueList.size - 1]
        assertThat(testArviointiosaalue.aluetunnus).isEqualTo(UPDATED_ALUETUNNUS)
        assertThat(testArviointiosaalue.nimi).isEqualTo(UPDATED_NIMI)
        assertThat(testArviointiosaalue.kuvaus).isEqualTo(UPDATED_KUVAUS)
        assertThat(testArviointiosaalue.osaamisenRajaarvo).isEqualTo(UPDATED_OSAAMISEN_RAJAARVO)
        assertThat(testArviointiosaalue.minimivaatimus).isEqualTo(UPDATED_MINIMIVAATIMUS)
        assertThat(testArviointiosaalue.voimassaoloAlkaa).isEqualTo(UPDATED_VOIMASSAOLO_ALKAA)
        assertThat(testArviointiosaalue.voimassaoloLoppuu).isEqualTo(UPDATED_VOIMASSAOLO_LOPPUU)
    }

    @Test
    @Transactional
    fun updateNonExistingArviointiosaalue() {
        val databaseSizeBeforeUpdate = arviointiosaalueRepository.findAll().size

        // Create the Arviointiosaalue
        val arviointiosaalueDTO = arviointiosaalueMapper.toDto(arviointiosaalue)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArviointiosaalueMockMvc.perform(
            put("/api/arviointiosaalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arviointiosaalueDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Arviointiosaalue in the database
        val arviointiosaalueList = arviointiosaalueRepository.findAll()
        assertThat(arviointiosaalueList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteArviointiosaalue() {
        // Initialize the database
        arviointiosaalueRepository.saveAndFlush(arviointiosaalue)

        val databaseSizeBeforeDelete = arviointiosaalueRepository.findAll().size

        // Delete the arviointiosaalue
        restArviointiosaalueMockMvc.perform(
            delete("/api/arviointiosaalues/{id}", arviointiosaalue.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val arviointiosaalueList = arviointiosaalueRepository.findAll()
        assertThat(arviointiosaalueList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_ALUETUNNUS = "AAAAAAAAAA"
        private const val UPDATED_ALUETUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

        private const val DEFAULT_OSAAMISEN_RAJAARVO = "AAAAAAAAAA"
        private const val UPDATED_OSAAMISEN_RAJAARVO = "BBBBBBBBBB"

        private const val DEFAULT_MINIMIVAATIMUS = "AAAAAAAAAA"
        private const val UPDATED_MINIMIVAATIMUS = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Arviointiosaalue {
            val arviointiosaalue = Arviointiosaalue(
                aluetunnus = DEFAULT_ALUETUNNUS,
                nimi = DEFAULT_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                osaamisenRajaarvo = DEFAULT_OSAAMISEN_RAJAARVO,
                minimivaatimus = DEFAULT_MINIMIVAATIMUS,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU
            )

            return arviointiosaalue
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Arviointiosaalue {
            val arviointiosaalue = Arviointiosaalue(
                aluetunnus = UPDATED_ALUETUNNUS,
                nimi = UPDATED_NIMI,
                kuvaus = UPDATED_KUVAUS,
                osaamisenRajaarvo = UPDATED_OSAAMISEN_RAJAARVO,
                minimivaatimus = UPDATED_MINIMIVAATIMUS,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
            )

            return arviointiosaalue
        }
    }
}
