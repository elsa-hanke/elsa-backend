package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue
import fi.elsapalvelu.elsa.domain.enumeration.CanmedsOsaalue
import fi.elsapalvelu.elsa.repository.ArvioitavaOsaalueRepository
import fi.elsapalvelu.elsa.service.ArvioitavaOsaalueService
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaOsaalueMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.createFormattingConversionService
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
 * Integration tests for the [ArvioitavaOsaalueResource] REST controller.
 *
 * @see ArvioitavaOsaalueResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class ArvioitavaOsaalueResourceIT {

    @Autowired
    private lateinit var arvioitavaOsaalueRepository: ArvioitavaOsaalueRepository

    @Autowired
    private lateinit var arvioitavaOsaalueMapper: ArvioitavaOsaalueMapper

    @Autowired
    private lateinit var arvioitavaOsaalueService: ArvioitavaOsaalueService

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

    private lateinit var restArvioitavaOsaalueMockMvc: MockMvc

    private lateinit var arvioitavaOsaalue: ArvioitavaOsaalue

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val arvioitavaOsaalueResource = ArvioitavaOsaalueResource(arvioitavaOsaalueService)
        this.restArvioitavaOsaalueMockMvc = MockMvcBuilders.standaloneSetup(arvioitavaOsaalueResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        arvioitavaOsaalue = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createArvioitavaOsaalue() {
        val databaseSizeBeforeCreate = arvioitavaOsaalueRepository.findAll().size

        // Create the ArvioitavaOsaalue
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)
        restArvioitavaOsaalueMockMvc.perform(
            post("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isCreated)

        // Validate the ArvioitavaOsaalue in the database
        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeCreate + 1)
        val testArvioitavaOsaalue = arvioitavaOsaalueList[arvioitavaOsaalueList.size - 1]
        assertThat(testArvioitavaOsaalue.tunnus).isEqualTo(DEFAULT_TUNNUS)
        assertThat(testArvioitavaOsaalue.nimi).isEqualTo(DEFAULT_NIMI)
        assertThat(testArvioitavaOsaalue.kuvaus).isEqualTo(DEFAULT_KUVAUS)
        assertThat(testArvioitavaOsaalue.osaamisenRajaarvo).isEqualTo(DEFAULT_OSAAMISEN_RAJAARVO)
        assertThat(testArvioitavaOsaalue.arviointikriteerit).isEqualTo(DEFAULT_ARVIOINTIKRITEERIT)
        assertThat(testArvioitavaOsaalue.voimassaoloAlkaa).isEqualTo(DEFAULT_VOIMASSAOLO_ALKAA)
        assertThat(testArvioitavaOsaalue.voimassaoloLoppuu).isEqualTo(DEFAULT_VOIMASSAOLO_LOPPUU)
        assertThat(testArvioitavaOsaalue.rooli).isEqualTo(DEFAULT_ROOLI)
    }

    @Test
    @Transactional
    fun createArvioitavaOsaalueWithExistingId() {
        val databaseSizeBeforeCreate = arvioitavaOsaalueRepository.findAll().size

        // Create the ArvioitavaOsaalue with an existing ID
        arvioitavaOsaalue.id = 1L
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)

        // An entity with an existing ID cannot be created, so this API call must fail
        restArvioitavaOsaalueMockMvc.perform(
            post("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isBadRequest)

        // Validate the ArvioitavaOsaalue in the database
        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkTunnusIsRequired() {
        val databaseSizeBeforeTest = arvioitavaOsaalueRepository.findAll().size
        // set the field null
        arvioitavaOsaalue.tunnus = null

        // Create the ArvioitavaOsaalue, which fails.
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)

        restArvioitavaOsaalueMockMvc.perform(
            post("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isBadRequest)

        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkNimiIsRequired() {
        val databaseSizeBeforeTest = arvioitavaOsaalueRepository.findAll().size
        // set the field null
        arvioitavaOsaalue.nimi = null

        // Create the ArvioitavaOsaalue, which fails.
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)

        restArvioitavaOsaalueMockMvc.perform(
            post("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isBadRequest)

        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkRooliIsRequired() {
        val databaseSizeBeforeTest = arvioitavaOsaalueRepository.findAll().size
        // set the field null
        arvioitavaOsaalue.rooli = null

        // Create the ArvioitavaOsaalue, which fails.
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)

        restArvioitavaOsaalueMockMvc.perform(
            post("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isBadRequest)

        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllArvioitavatOsaalueet() {
        // Initialize the database
        arvioitavaOsaalueRepository.saveAndFlush(arvioitavaOsaalue)

        // Get all the arvioitavaOsaalueList
        restArvioitavaOsaalueMockMvc.perform(get("/api/arvioitavat-osaalueet?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(arvioitavaOsaalue.id?.toInt())))
            .andExpect(jsonPath("$.[*].tunnus").value(hasItem(DEFAULT_TUNNUS)))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
            .andExpect(jsonPath("$.[*].kuvaus").value(hasItem(DEFAULT_KUVAUS)))
            .andExpect(jsonPath("$.[*].osaamisenRajaarvo").value(hasItem(DEFAULT_OSAAMISEN_RAJAARVO)))
            .andExpect(jsonPath("$.[*].arviointikriteerit").value(hasItem(DEFAULT_ARVIOINTIKRITEERIT)))
            .andExpect(jsonPath("$.[*].voimassaoloAlkaa").value(hasItem(DEFAULT_VOIMASSAOLO_ALKAA.toString())))
            .andExpect(jsonPath("$.[*].voimassaoloLoppuu").value(hasItem(DEFAULT_VOIMASSAOLO_LOPPUU.toString())))
            .andExpect(jsonPath("$.[*].rooli").value(hasItem(DEFAULT_ROOLI.toString())))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getArvioitavaOsaalue() {
        // Initialize the database
        arvioitavaOsaalueRepository.saveAndFlush(arvioitavaOsaalue)

        val id = arvioitavaOsaalue.id
        assertNotNull(id)

        // Get the arvioitavaOsaalue
        restArvioitavaOsaalueMockMvc.perform(get("/api/arvioitavat-osaalueet/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(arvioitavaOsaalue.id as Any))
            .andExpect(jsonPath("$.tunnus").value(DEFAULT_TUNNUS))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
            .andExpect(jsonPath("$.kuvaus").value(DEFAULT_KUVAUS))
            .andExpect(jsonPath("$.osaamisenRajaarvo").value(DEFAULT_OSAAMISEN_RAJAARVO))
            .andExpect(jsonPath("$.arviointikriteerit").value(DEFAULT_ARVIOINTIKRITEERIT))
            .andExpect(jsonPath("$.voimassaoloAlkaa").value(DEFAULT_VOIMASSAOLO_ALKAA.toString()))
            .andExpect(jsonPath("$.voimassaoloLoppuu").value(DEFAULT_VOIMASSAOLO_LOPPUU.toString()))
            .andExpect(jsonPath("$.rooli").value(DEFAULT_ROOLI.toString()))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingArvioitavaOsaalue() {
        // Get the arvioitavaOsaalue
        restArvioitavaOsaalueMockMvc.perform(get("/api/arvioitavat-osaalueet/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateArvioitavaOsaalue() {
        // Initialize the database
        arvioitavaOsaalueRepository.saveAndFlush(arvioitavaOsaalue)

        val databaseSizeBeforeUpdate = arvioitavaOsaalueRepository.findAll().size

        // Update the arvioitavaOsaalue
        val id = arvioitavaOsaalue.id
        assertNotNull(id)
        val updatedArvioitavaOsaalue = arvioitavaOsaalueRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedArvioitavaOsaalue are not directly saved in db
        em.detach(updatedArvioitavaOsaalue)
        updatedArvioitavaOsaalue.tunnus = UPDATED_TUNNUS
        updatedArvioitavaOsaalue.nimi = UPDATED_NIMI
        updatedArvioitavaOsaalue.kuvaus = UPDATED_KUVAUS
        updatedArvioitavaOsaalue.osaamisenRajaarvo = UPDATED_OSAAMISEN_RAJAARVO
        updatedArvioitavaOsaalue.arviointikriteerit = UPDATED_ARVIOINTIKRITEERIT
        updatedArvioitavaOsaalue.voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA
        updatedArvioitavaOsaalue.voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
        updatedArvioitavaOsaalue.rooli = UPDATED_ROOLI
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(updatedArvioitavaOsaalue)

        restArvioitavaOsaalueMockMvc.perform(
            put("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isOk)

        // Validate the ArvioitavaOsaalue in the database
        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeUpdate)
        val testArvioitavaOsaalue = arvioitavaOsaalueList[arvioitavaOsaalueList.size - 1]
        assertThat(testArvioitavaOsaalue.tunnus).isEqualTo(UPDATED_TUNNUS)
        assertThat(testArvioitavaOsaalue.nimi).isEqualTo(UPDATED_NIMI)
        assertThat(testArvioitavaOsaalue.kuvaus).isEqualTo(UPDATED_KUVAUS)
        assertThat(testArvioitavaOsaalue.osaamisenRajaarvo).isEqualTo(UPDATED_OSAAMISEN_RAJAARVO)
        assertThat(testArvioitavaOsaalue.arviointikriteerit).isEqualTo(UPDATED_ARVIOINTIKRITEERIT)
        assertThat(testArvioitavaOsaalue.voimassaoloAlkaa).isEqualTo(UPDATED_VOIMASSAOLO_ALKAA)
        assertThat(testArvioitavaOsaalue.voimassaoloLoppuu).isEqualTo(UPDATED_VOIMASSAOLO_LOPPUU)
        assertThat(testArvioitavaOsaalue.rooli).isEqualTo(UPDATED_ROOLI)
    }

    @Test
    @Transactional
    fun updateNonExistingArvioitavaOsaalue() {
        val databaseSizeBeforeUpdate = arvioitavaOsaalueRepository.findAll().size

        // Create the ArvioitavaOsaalue
        val arvioitavaOsaalueDTO = arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArvioitavaOsaalueMockMvc.perform(
            put("/api/arvioitavat-osaalueet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(arvioitavaOsaalueDTO))
        ).andExpect(status().isBadRequest)

        // Validate the ArvioitavaOsaalue in the database
        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteArvioitavaOsaalue() {
        // Initialize the database
        arvioitavaOsaalueRepository.saveAndFlush(arvioitavaOsaalue)

        val databaseSizeBeforeDelete = arvioitavaOsaalueRepository.findAll().size

        // Delete the arvioitavaOsaalue
        restArvioitavaOsaalueMockMvc.perform(
            delete("/api/arvioitavat-osaalueet/{id}", arvioitavaOsaalue.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val arvioitavaOsaalueList = arvioitavaOsaalueRepository.findAll()
        assertThat(arvioitavaOsaalueList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_TUNNUS = "AAAAAAAAAA"
        private const val UPDATED_TUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

        private const val DEFAULT_OSAAMISEN_RAJAARVO = "AAAAAAAAAA"
        private const val UPDATED_OSAAMISEN_RAJAARVO = "BBBBBBBBBB"

        private const val DEFAULT_ARVIOINTIKRITEERIT = "AAAAAAAAAA"
        private const val UPDATED_ARVIOINTIKRITEERIT = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_ROOLI: CanmedsOsaalue = CanmedsOsaalue.AMMATILLISUUS
        private val UPDATED_ROOLI: CanmedsOsaalue = CanmedsOsaalue.VUOROVAIKUTUSTAIDOT

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): ArvioitavaOsaalue {
            val arvioitavaOsaalue = ArvioitavaOsaalue(
                tunnus = DEFAULT_TUNNUS,
                nimi = DEFAULT_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                osaamisenRajaarvo = DEFAULT_OSAAMISEN_RAJAARVO,
                arviointikriteerit = DEFAULT_ARVIOINTIKRITEERIT,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU,
                rooli = DEFAULT_ROOLI
            )

            return arvioitavaOsaalue
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(): ArvioitavaOsaalue {
            val arvioitavaOsaalue = ArvioitavaOsaalue(
                tunnus = UPDATED_TUNNUS,
                nimi = UPDATED_NIMI,
                kuvaus = UPDATED_KUVAUS,
                osaamisenRajaarvo = UPDATED_OSAAMISEN_RAJAARVO,
                arviointikriteerit = UPDATED_ARVIOINTIKRITEERIT,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU,
                rooli = UPDATED_ROOLI
            )

            return arvioitavaOsaalue
        }
    }
}
