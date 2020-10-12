package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import fi.elsapalvelu.elsa.repository.EpaOsaamisalueRepository
import fi.elsapalvelu.elsa.service.EpaOsaamisalueService
import fi.elsapalvelu.elsa.service.mapper.EpaOsaamisalueMapper
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
import java.time.LocalDate
import java.time.ZoneId
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
 * Integration tests for the [EpaOsaamisalueResource] REST controller.
 *
 * @see EpaOsaamisalueResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class EpaOsaamisalueResourceIT {

    @Autowired
    private lateinit var epaOsaamisalueRepository: EpaOsaamisalueRepository

    @Autowired
    private lateinit var epaOsaamisalueMapper: EpaOsaamisalueMapper

    @Autowired
    private lateinit var epaOsaamisalueService: EpaOsaamisalueService

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

    private lateinit var restEpaOsaamisalueMockMvc: MockMvc

    private lateinit var epaOsaamisalue: EpaOsaamisalue

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val epaOsaamisalueResource = EpaOsaamisalueResource(epaOsaamisalueService)
         this.restEpaOsaamisalueMockMvc = MockMvcBuilders.standaloneSetup(epaOsaamisalueResource)
             .setCustomArgumentResolvers(pageableArgumentResolver)
             .setControllerAdvice(exceptionTranslator)
             .setConversionService(createFormattingConversionService())
             .setMessageConverters(jacksonMessageConverter)
             .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        epaOsaamisalue = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createEpaOsaamisalue() {
        val databaseSizeBeforeCreate = epaOsaamisalueRepository.findAll().size

        // Create the EpaOsaamisalue
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(epaOsaamisalue)
        restEpaOsaamisalueMockMvc.perform(
            post("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isCreated)

        // Validate the EpaOsaamisalue in the database
        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeCreate + 1)
        val testEpaOsaamisalue = epaOsaamisalueList[epaOsaamisalueList.size - 1]
        assertThat(testEpaOsaamisalue.epaTunnus).isEqualTo(DEFAULT_EPA_TUNNUS)
        assertThat(testEpaOsaamisalue.epaNimi).isEqualTo(DEFAULT_EPA_NIMI)
        assertThat(testEpaOsaamisalue.kuvaus).isEqualTo(DEFAULT_KUVAUS)
        assertThat(testEpaOsaamisalue.voimassaoloAlkaa).isEqualTo(DEFAULT_VOIMASSAOLO_ALKAA)
        assertThat(testEpaOsaamisalue.voimassaoloLoppuu).isEqualTo(DEFAULT_VOIMASSAOLO_LOPPUU)
    }

    @Test
    @Transactional
    fun createEpaOsaamisalueWithExistingId() {
        val databaseSizeBeforeCreate = epaOsaamisalueRepository.findAll().size

        // Create the EpaOsaamisalue with an existing ID
        epaOsaamisalue.id = 1L
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(epaOsaamisalue)

        // An entity with an existing ID cannot be created, so this API call must fail
        restEpaOsaamisalueMockMvc.perform(
            post("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isBadRequest)

        // Validate the EpaOsaamisalue in the database
        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkEpaTunnusIsRequired() {
        val databaseSizeBeforeTest = epaOsaamisalueRepository.findAll().size
        // set the field null
        epaOsaamisalue.epaTunnus = null

        // Create the EpaOsaamisalue, which fails.
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(epaOsaamisalue)

        restEpaOsaamisalueMockMvc.perform(
            post("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isBadRequest)

        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkEpaNimiIsRequired() {
        val databaseSizeBeforeTest = epaOsaamisalueRepository.findAll().size
        // set the field null
        epaOsaamisalue.epaNimi = null

        // Create the EpaOsaamisalue, which fails.
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(epaOsaamisalue)

        restEpaOsaamisalueMockMvc.perform(
            post("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isBadRequest)

        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkVoimassaoloAlkaaIsRequired() {
        val databaseSizeBeforeTest = epaOsaamisalueRepository.findAll().size
        // set the field null
        epaOsaamisalue.voimassaoloAlkaa = null

        // Create the EpaOsaamisalue, which fails.
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(epaOsaamisalue)

        restEpaOsaamisalueMockMvc.perform(
            post("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isBadRequest)

        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllEpaOsaamisalues() {
        // Initialize the database
        epaOsaamisalueRepository.saveAndFlush(epaOsaamisalue)

        // Get all the epaOsaamisalueList
        restEpaOsaamisalueMockMvc.perform(get("/api/epa-osaamisalues?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(epaOsaamisalue.id?.toInt())))
            .andExpect(jsonPath("$.[*].epaTunnus").value(hasItem(DEFAULT_EPA_TUNNUS)))
            .andExpect(jsonPath("$.[*].epaNimi").value(hasItem(DEFAULT_EPA_NIMI)))
            .andExpect(jsonPath("$.[*].kuvaus").value(hasItem(DEFAULT_KUVAUS)))
            .andExpect(jsonPath("$.[*].voimassaoloAlkaa").value(hasItem(DEFAULT_VOIMASSAOLO_ALKAA.toString())))
            .andExpect(jsonPath("$.[*].voimassaoloLoppuu").value(hasItem(DEFAULT_VOIMASSAOLO_LOPPUU.toString()))) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getEpaOsaamisalue() {
        // Initialize the database
        epaOsaamisalueRepository.saveAndFlush(epaOsaamisalue)

        val id = epaOsaamisalue.id
        assertNotNull(id)

        // Get the epaOsaamisalue
        restEpaOsaamisalueMockMvc.perform(get("/api/epa-osaamisalues/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(epaOsaamisalue.id?.toInt()))
            .andExpect(jsonPath("$.epaTunnus").value(DEFAULT_EPA_TUNNUS))
            .andExpect(jsonPath("$.epaNimi").value(DEFAULT_EPA_NIMI))
            .andExpect(jsonPath("$.kuvaus").value(DEFAULT_KUVAUS))
            .andExpect(jsonPath("$.voimassaoloAlkaa").value(DEFAULT_VOIMASSAOLO_ALKAA.toString()))
            .andExpect(jsonPath("$.voimassaoloLoppuu").value(DEFAULT_VOIMASSAOLO_LOPPUU.toString())) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingEpaOsaamisalue() {
        // Get the epaOsaamisalue
        restEpaOsaamisalueMockMvc.perform(get("/api/epa-osaamisalues/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateEpaOsaamisalue() {
        // Initialize the database
        epaOsaamisalueRepository.saveAndFlush(epaOsaamisalue)

        val databaseSizeBeforeUpdate = epaOsaamisalueRepository.findAll().size

        // Update the epaOsaamisalue
        val id = epaOsaamisalue.id
        assertNotNull(id)
        val updatedEpaOsaamisalue = epaOsaamisalueRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedEpaOsaamisalue are not directly saved in db
        em.detach(updatedEpaOsaamisalue)
        updatedEpaOsaamisalue.epaTunnus = UPDATED_EPA_TUNNUS
        updatedEpaOsaamisalue.epaNimi = UPDATED_EPA_NIMI
        updatedEpaOsaamisalue.kuvaus = UPDATED_KUVAUS
        updatedEpaOsaamisalue.voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA
        updatedEpaOsaamisalue.voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(updatedEpaOsaamisalue)

        restEpaOsaamisalueMockMvc.perform(
            put("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isOk)

        // Validate the EpaOsaamisalue in the database
        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeUpdate)
        val testEpaOsaamisalue = epaOsaamisalueList[epaOsaamisalueList.size - 1]
        assertThat(testEpaOsaamisalue.epaTunnus).isEqualTo(UPDATED_EPA_TUNNUS)
        assertThat(testEpaOsaamisalue.epaNimi).isEqualTo(UPDATED_EPA_NIMI)
        assertThat(testEpaOsaamisalue.kuvaus).isEqualTo(UPDATED_KUVAUS)
        assertThat(testEpaOsaamisalue.voimassaoloAlkaa).isEqualTo(UPDATED_VOIMASSAOLO_ALKAA)
        assertThat(testEpaOsaamisalue.voimassaoloLoppuu).isEqualTo(UPDATED_VOIMASSAOLO_LOPPUU)
    }

    @Test
    @Transactional
    fun updateNonExistingEpaOsaamisalue() {
        val databaseSizeBeforeUpdate = epaOsaamisalueRepository.findAll().size

        // Create the EpaOsaamisalue
        val epaOsaamisalueDTO = epaOsaamisalueMapper.toDto(epaOsaamisalue)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEpaOsaamisalueMockMvc.perform(
            put("/api/epa-osaamisalues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(epaOsaamisalueDTO))
        ).andExpect(status().isBadRequest)

        // Validate the EpaOsaamisalue in the database
        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteEpaOsaamisalue() {
        // Initialize the database
        epaOsaamisalueRepository.saveAndFlush(epaOsaamisalue)

        val databaseSizeBeforeDelete = epaOsaamisalueRepository.findAll().size

        // Delete the epaOsaamisalue
        restEpaOsaamisalueMockMvc.perform(
            delete("/api/epa-osaamisalues/{id}", epaOsaamisalue.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val epaOsaamisalueList = epaOsaamisalueRepository.findAll()
        assertThat(epaOsaamisalueList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_EPA_TUNNUS = "AAAAAAAAAA"
        private const val UPDATED_EPA_TUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_EPA_NIMI = "AAAAAAAAAA"
        private const val UPDATED_EPA_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

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
        fun createEntity(em: EntityManager): EpaOsaamisalue {
            val epaOsaamisalue = EpaOsaamisalue(
                epaTunnus = DEFAULT_EPA_TUNNUS,
                epaNimi = DEFAULT_EPA_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU
            )

            return epaOsaamisalue
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): EpaOsaamisalue {
            val epaOsaamisalue = EpaOsaamisalue(
                epaTunnus = UPDATED_EPA_TUNNUS,
                epaNimi = UPDATED_EPA_NIMI,
                kuvaus = UPDATED_KUVAUS,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
            )

            return epaOsaamisalue
        }
    }
}
