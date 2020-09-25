package fi.oulu.elsa.web.rest

import fi.oulu.elsa.ElsaBackendApp
import fi.oulu.elsa.config.TestSecurityConfiguration
import fi.oulu.elsa.domain.Osoite
import fi.oulu.elsa.repository.OsoiteRepository
import fi.oulu.elsa.service.OsoiteService
import fi.oulu.elsa.service.mapper.OsoiteMapper
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
 * Integration tests for the [OsoiteResource] REST controller.
 *
 * @see OsoiteResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class OsoiteResourceIT {

    @Autowired
    private lateinit var osoiteRepository: OsoiteRepository

    @Autowired
    private lateinit var osoiteMapper: OsoiteMapper

    @Autowired
    private lateinit var osoiteService: OsoiteService

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

    private lateinit var restOsoiteMockMvc: MockMvc

    private lateinit var osoite: Osoite

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val osoiteResource = OsoiteResource(osoiteService)
        this.restOsoiteMockMvc = MockMvcBuilders.standaloneSetup(osoiteResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        osoite = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createOsoite() {
        val databaseSizeBeforeCreate = osoiteRepository.findAll().size

        // Create the Osoite
        val osoiteDTO = osoiteMapper.toDto(osoite)
        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isCreated)

        // Validate the Osoite in the database
        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeCreate + 1)
        val testOsoite = osoiteList[osoiteList.size - 1]
        assertThat(testOsoite.ensisijainen).isEqualTo(DEFAULT_ENSISIJAINEN)
        assertThat(testOsoite.osoiterivi1).isEqualTo(DEFAULT_OSOITERIVI_1)
        assertThat(testOsoite.osoiterivi2).isEqualTo(DEFAULT_OSOITERIVI_2)
        assertThat(testOsoite.osoiterivi3).isEqualTo(DEFAULT_OSOITERIVI_3)
        assertThat(testOsoite.kunta).isEqualTo(DEFAULT_KUNTA)
        assertThat(testOsoite.postinumero).isEqualTo(DEFAULT_POSTINUMERO)
        assertThat(testOsoite.maakunta).isEqualTo(DEFAULT_MAAKUNTA)
        assertThat(testOsoite.maa).isEqualTo(DEFAULT_MAA)
    }

    @Test
    @Transactional
    fun createOsoiteWithExistingId() {
        val databaseSizeBeforeCreate = osoiteRepository.findAll().size

        // Create the Osoite with an existing ID
        osoite.id = 1L
        val osoiteDTO = osoiteMapper.toDto(osoite)

        // An entity with an existing ID cannot be created, so this API call must fail
        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Osoite in the database
        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkEnsisijainenIsRequired() {
        val databaseSizeBeforeTest = osoiteRepository.findAll().size
        // set the field null
        osoite.ensisijainen = null

        // Create the Osoite, which fails.
        val osoiteDTO = osoiteMapper.toDto(osoite)

        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkOsoiterivi1IsRequired() {
        val databaseSizeBeforeTest = osoiteRepository.findAll().size
        // set the field null
        osoite.osoiterivi1 = null

        // Create the Osoite, which fails.
        val osoiteDTO = osoiteMapper.toDto(osoite)

        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkKuntaIsRequired() {
        val databaseSizeBeforeTest = osoiteRepository.findAll().size
        // set the field null
        osoite.kunta = null

        // Create the Osoite, which fails.
        val osoiteDTO = osoiteMapper.toDto(osoite)

        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkPostinumeroIsRequired() {
        val databaseSizeBeforeTest = osoiteRepository.findAll().size
        // set the field null
        osoite.postinumero = null

        // Create the Osoite, which fails.
        val osoiteDTO = osoiteMapper.toDto(osoite)

        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkMaaIsRequired() {
        val databaseSizeBeforeTest = osoiteRepository.findAll().size
        // set the field null
        osoite.maa = null

        // Create the Osoite, which fails.
        val osoiteDTO = osoiteMapper.toDto(osoite)

        restOsoiteMockMvc.perform(
            post("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllOsoites() {
        // Initialize the database
        osoiteRepository.saveAndFlush(osoite)

        // Get all the osoiteList
        restOsoiteMockMvc.perform(get("/api/osoites?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(osoite.id?.toInt())))
            .andExpect(jsonPath("$.[*].ensisijainen").value(hasItem(DEFAULT_ENSISIJAINEN)))
            .andExpect(jsonPath("$.[*].osoiterivi1").value(hasItem(DEFAULT_OSOITERIVI_1)))
            .andExpect(jsonPath("$.[*].osoiterivi2").value(hasItem(DEFAULT_OSOITERIVI_2)))
            .andExpect(jsonPath("$.[*].osoiterivi3").value(hasItem(DEFAULT_OSOITERIVI_3)))
            .andExpect(jsonPath("$.[*].kunta").value(hasItem(DEFAULT_KUNTA)))
            .andExpect(jsonPath("$.[*].postinumero").value(hasItem(DEFAULT_POSTINUMERO)))
            .andExpect(jsonPath("$.[*].maakunta").value(hasItem(DEFAULT_MAAKUNTA)))
            .andExpect(jsonPath("$.[*].maa").value(hasItem(DEFAULT_MAA)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getOsoite() {
        // Initialize the database
        osoiteRepository.saveAndFlush(osoite)

        val id = osoite.id
        assertNotNull(id)

        // Get the osoite
        restOsoiteMockMvc.perform(get("/api/osoites/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(osoite.id?.toInt()))
            .andExpect(jsonPath("$.ensisijainen").value(DEFAULT_ENSISIJAINEN))
            .andExpect(jsonPath("$.osoiterivi1").value(DEFAULT_OSOITERIVI_1))
            .andExpect(jsonPath("$.osoiterivi2").value(DEFAULT_OSOITERIVI_2))
            .andExpect(jsonPath("$.osoiterivi3").value(DEFAULT_OSOITERIVI_3))
            .andExpect(jsonPath("$.kunta").value(DEFAULT_KUNTA))
            .andExpect(jsonPath("$.postinumero").value(DEFAULT_POSTINUMERO))
            .andExpect(jsonPath("$.maakunta").value(DEFAULT_MAAKUNTA))
            .andExpect(jsonPath("$.maa").value(DEFAULT_MAA))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingOsoite() {
        // Get the osoite
        restOsoiteMockMvc.perform(get("/api/osoites/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateOsoite() {
        // Initialize the database
        osoiteRepository.saveAndFlush(osoite)

        val databaseSizeBeforeUpdate = osoiteRepository.findAll().size

        // Update the osoite
        val id = osoite.id
        assertNotNull(id)
        val updatedOsoite = osoiteRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedOsoite are not directly saved in db
        em.detach(updatedOsoite)
        updatedOsoite.ensisijainen = UPDATED_ENSISIJAINEN
        updatedOsoite.osoiterivi1 = UPDATED_OSOITERIVI_1
        updatedOsoite.osoiterivi2 = UPDATED_OSOITERIVI_2
        updatedOsoite.osoiterivi3 = UPDATED_OSOITERIVI_3
        updatedOsoite.kunta = UPDATED_KUNTA
        updatedOsoite.postinumero = UPDATED_POSTINUMERO
        updatedOsoite.maakunta = UPDATED_MAAKUNTA
        updatedOsoite.maa = UPDATED_MAA
        val osoiteDTO = osoiteMapper.toDto(updatedOsoite)

        restOsoiteMockMvc.perform(
            put("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isOk)

        // Validate the Osoite in the database
        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeUpdate)
        val testOsoite = osoiteList[osoiteList.size - 1]
        assertThat(testOsoite.ensisijainen).isEqualTo(UPDATED_ENSISIJAINEN)
        assertThat(testOsoite.osoiterivi1).isEqualTo(UPDATED_OSOITERIVI_1)
        assertThat(testOsoite.osoiterivi2).isEqualTo(UPDATED_OSOITERIVI_2)
        assertThat(testOsoite.osoiterivi3).isEqualTo(UPDATED_OSOITERIVI_3)
        assertThat(testOsoite.kunta).isEqualTo(UPDATED_KUNTA)
        assertThat(testOsoite.postinumero).isEqualTo(UPDATED_POSTINUMERO)
        assertThat(testOsoite.maakunta).isEqualTo(UPDATED_MAAKUNTA)
        assertThat(testOsoite.maa).isEqualTo(UPDATED_MAA)
    }

    @Test
    @Transactional
    fun updateNonExistingOsoite() {
        val databaseSizeBeforeUpdate = osoiteRepository.findAll().size

        // Create the Osoite
        val osoiteDTO = osoiteMapper.toDto(osoite)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOsoiteMockMvc.perform(
            put("/api/osoites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(osoiteDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Osoite in the database
        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteOsoite() {
        // Initialize the database
        osoiteRepository.saveAndFlush(osoite)

        val databaseSizeBeforeDelete = osoiteRepository.findAll().size

        // Delete the osoite
        restOsoiteMockMvc.perform(
            delete("/api/osoites/{id}", osoite.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val osoiteList = osoiteRepository.findAll()
        assertThat(osoiteList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_ENSISIJAINEN: Boolean = false
        private const val UPDATED_ENSISIJAINEN: Boolean = true

        private const val DEFAULT_OSOITERIVI_1 = "AAAAAAAAAA"
        private const val UPDATED_OSOITERIVI_1 = "BBBBBBBBBB"

        private const val DEFAULT_OSOITERIVI_2 = "AAAAAAAAAA"
        private const val UPDATED_OSOITERIVI_2 = "BBBBBBBBBB"

        private const val DEFAULT_OSOITERIVI_3 = "AAAAAAAAAA"
        private const val UPDATED_OSOITERIVI_3 = "BBBBBBBBBB"

        private const val DEFAULT_KUNTA = "AAAAAAAAAA"
        private const val UPDATED_KUNTA = "BBBBBBBBBB"

        private const val DEFAULT_POSTINUMERO: Int = 1
        private const val UPDATED_POSTINUMERO: Int = 2

        private const val DEFAULT_MAAKUNTA = "AAAAAAAAAA"
        private const val UPDATED_MAAKUNTA = "BBBBBBBBBB"

        private const val DEFAULT_MAA = "AAAAAAAAAA"
        private const val UPDATED_MAA = "BBBBBBBBBB"

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Osoite {
            val osoite = Osoite(
                ensisijainen = DEFAULT_ENSISIJAINEN,
                osoiterivi1 = DEFAULT_OSOITERIVI_1,
                osoiterivi2 = DEFAULT_OSOITERIVI_2,
                osoiterivi3 = DEFAULT_OSOITERIVI_3,
                kunta = DEFAULT_KUNTA,
                postinumero = DEFAULT_POSTINUMERO,
                maakunta = DEFAULT_MAAKUNTA,
                maa = DEFAULT_MAA
            )

            return osoite
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Osoite {
            val osoite = Osoite(
                ensisijainen = UPDATED_ENSISIJAINEN,
                osoiterivi1 = UPDATED_OSOITERIVI_1,
                osoiterivi2 = UPDATED_OSOITERIVI_2,
                osoiterivi3 = UPDATED_OSOITERIVI_3,
                kunta = UPDATED_KUNTA,
                postinumero = UPDATED_POSTINUMERO,
                maakunta = UPDATED_MAAKUNTA,
                maa = UPDATED_MAA
            )

            return osoite
        }
    }
}
