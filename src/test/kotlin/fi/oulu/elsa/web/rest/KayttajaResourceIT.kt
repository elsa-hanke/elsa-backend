package fi.oulu.elsa.web.rest

import fi.oulu.elsa.BackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Kayttaja
import fi.oulu.elsa.domain.enumeration.Kieli
import fi.oulu.elsa.repository.KayttajaRepository
import fi.oulu.elsa.service.KayttajaService
import fi.oulu.elsa.service.mapper.KayttajaMapper
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
import org.springframework.util.Base64Utils
import org.springframework.validation.Validator
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

/**
 * Integration tests for the [KayttajaResource] REST controller.
 *
 * @see KayttajaResource
 */
@SpringBootTest(classes = [BackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class KayttajaResourceIT {

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    @Autowired
    private lateinit var kayttajaMapper: KayttajaMapper

    @Autowired
    private lateinit var kayttajaService: KayttajaService

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

    private lateinit var restKayttajaMockMvc: MockMvc

    private lateinit var kayttaja: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val kayttajaResource = KayttajaResource(kayttajaService)
        this.restKayttajaMockMvc = MockMvcBuilders.standaloneSetup(kayttajaResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        kayttaja = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createKayttaja() {
        val databaseSizeBeforeCreate = kayttajaRepository.findAll().size

        // Create the Kayttaja
        val kayttajaDTO = kayttajaMapper.toDto(kayttaja)
        restKayttajaMockMvc.perform(
            post("/api/kayttajas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
        ).andExpect(status().isCreated)

        // Validate the Kayttaja in the database
        val kayttajaList = kayttajaRepository.findAll()
        assertThat(kayttajaList).hasSize(databaseSizeBeforeCreate + 1)
        val testKayttaja = kayttajaList[kayttajaList.size - 1]
        assertThat(testKayttaja.nimi).isEqualTo(DEFAULT_NIMI)
        assertThat(testKayttaja.profiilikuva).isEqualTo(DEFAULT_PROFIILIKUVA)
        assertThat(testKayttaja.profiilikuvaContentType).isEqualTo(DEFAULT_PROFIILIKUVA_CONTENT_TYPE)
        assertThat(testKayttaja.kieli).isEqualTo(DEFAULT_KIELI)
    }

    @Test
    @Transactional
    fun createKayttajaWithExistingId() {
        val databaseSizeBeforeCreate = kayttajaRepository.findAll().size

        // Create the Kayttaja with an existing ID
        kayttaja.id = 1L
        val kayttajaDTO = kayttajaMapper.toDto(kayttaja)

        // An entity with an existing ID cannot be created, so this API call must fail
        restKayttajaMockMvc.perform(
            post("/api/kayttajas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Kayttaja in the database
        val kayttajaList = kayttajaRepository.findAll()
        assertThat(kayttajaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkNimiIsRequired() {
        val databaseSizeBeforeTest = kayttajaRepository.findAll().size
        // set the field null
        kayttaja.nimi = null

        // Create the Kayttaja, which fails.
        val kayttajaDTO = kayttajaMapper.toDto(kayttaja)

        restKayttajaMockMvc.perform(
            post("/api/kayttajas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
        ).andExpect(status().isBadRequest)

        val kayttajaList = kayttajaRepository.findAll()
        assertThat(kayttajaList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllKayttajas() {
        // Initialize the database
        kayttajaRepository.saveAndFlush(kayttaja)

        // Get all the kayttajaList
        restKayttajaMockMvc.perform(get("/api/kayttajas?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(kayttaja.id?.toInt())))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
            .andExpect(jsonPath("$.[*].profiilikuvaContentType").value(hasItem(DEFAULT_PROFIILIKUVA_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].profiilikuva").value(hasItem(Base64Utils.encodeToString(DEFAULT_PROFIILIKUVA))))
            .andExpect(jsonPath("$.[*].kieli").value(hasItem(DEFAULT_KIELI.toString())))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getKayttaja() {
        // Initialize the database
        kayttajaRepository.saveAndFlush(kayttaja)

        val id = kayttaja.id
        assertNotNull(id)

        // Get the kayttaja
        restKayttajaMockMvc.perform(get("/api/kayttajas/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(kayttaja.id?.toInt()))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
            .andExpect(jsonPath("$.profiilikuvaContentType").value(DEFAULT_PROFIILIKUVA_CONTENT_TYPE))
            .andExpect(jsonPath("$.profiilikuva").value(Base64Utils.encodeToString(DEFAULT_PROFIILIKUVA)))
            .andExpect(jsonPath("$.kieli").value(DEFAULT_KIELI.toString()))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingKayttaja() {
        // Get the kayttaja
        restKayttajaMockMvc.perform(get("/api/kayttajas/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateKayttaja() {
        // Initialize the database
        kayttajaRepository.saveAndFlush(kayttaja)

        val databaseSizeBeforeUpdate = kayttajaRepository.findAll().size

        // Update the kayttaja
        val id = kayttaja.id
        assertNotNull(id)
        val updatedKayttaja = kayttajaRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedKayttaja are not directly saved in db
        em.detach(updatedKayttaja)
        updatedKayttaja.nimi = UPDATED_NIMI
        updatedKayttaja.profiilikuva = UPDATED_PROFIILIKUVA
        updatedKayttaja.profiilikuvaContentType = UPDATED_PROFIILIKUVA_CONTENT_TYPE
        updatedKayttaja.kieli = UPDATED_KIELI
        val kayttajaDTO = kayttajaMapper.toDto(updatedKayttaja)

        restKayttajaMockMvc.perform(
            put("/api/kayttajas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
        ).andExpect(status().isOk)

        // Validate the Kayttaja in the database
        val kayttajaList = kayttajaRepository.findAll()
        assertThat(kayttajaList).hasSize(databaseSizeBeforeUpdate)
        val testKayttaja = kayttajaList[kayttajaList.size - 1]
        assertThat(testKayttaja.nimi).isEqualTo(UPDATED_NIMI)
        assertThat(testKayttaja.profiilikuva).isEqualTo(UPDATED_PROFIILIKUVA)
        assertThat(testKayttaja.profiilikuvaContentType).isEqualTo(UPDATED_PROFIILIKUVA_CONTENT_TYPE)
        assertThat(testKayttaja.kieli).isEqualTo(UPDATED_KIELI)
    }

    @Test
    @Transactional
    fun updateNonExistingKayttaja() {
        val databaseSizeBeforeUpdate = kayttajaRepository.findAll().size

        // Create the Kayttaja
        val kayttajaDTO = kayttajaMapper.toDto(kayttaja)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restKayttajaMockMvc.perform(
            put("/api/kayttajas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajaDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Kayttaja in the database
        val kayttajaList = kayttajaRepository.findAll()
        assertThat(kayttajaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteKayttaja() {
        // Initialize the database
        kayttajaRepository.saveAndFlush(kayttaja)

        val databaseSizeBeforeDelete = kayttajaRepository.findAll().size

        // Delete the kayttaja
        restKayttajaMockMvc.perform(
            delete("/api/kayttajas/{id}", kayttaja.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val kayttajaList = kayttajaRepository.findAll()
        assertThat(kayttajaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_PROFIILIKUVA: ByteArray = createByteArray(1, "0")
        private val UPDATED_PROFIILIKUVA: ByteArray = createByteArray(1, "1")
        private const val DEFAULT_PROFIILIKUVA_CONTENT_TYPE: String = "image/jpg"
        private const val UPDATED_PROFIILIKUVA_CONTENT_TYPE: String = "image/png"

        private val DEFAULT_KIELI: Kieli = Kieli.SUOMI
        private val UPDATED_KIELI: Kieli = Kieli.RUOTSI

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Kayttaja {
            val kayttaja = Kayttaja(
                nimi = DEFAULT_NIMI,
                profiilikuva = DEFAULT_PROFIILIKUVA,
                profiilikuvaContentType = DEFAULT_PROFIILIKUVA_CONTENT_TYPE,
                kieli = DEFAULT_KIELI
            )

            return kayttaja
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Kayttaja {
            val kayttaja = Kayttaja(
                nimi = UPDATED_NIMI,
                profiilikuva = UPDATED_PROFIILIKUVA,
                profiilikuvaContentType = UPDATED_PROFIILIKUVA_CONTENT_TYPE,
                kieli = UPDATED_KIELI
            )

            return kayttaja
        }
    }
}
