package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.mapper.ErikoistuvaLaakariMapper
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
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

/**
 * Integration tests for the [ErikoistuvaLaakariResource] REST controller.
 *
 * @see ErikoistuvaLaakariResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class ErikoistuvaLaakariResourceIT {

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper

    @Autowired
    private lateinit var erikoistuvaLaakariService: ErikoistuvaLaakariService

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

    private lateinit var restErikoistuvaLaakariMockMvc: MockMvc

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val erikoistuvaLaakariResource = ErikoistuvaLaakariResource(erikoistuvaLaakariService)
        this.restErikoistuvaLaakariMockMvc = MockMvcBuilders.standaloneSetup(erikoistuvaLaakariResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        erikoistuvaLaakari = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createErikoistuvaLaakari() {
        val databaseSizeBeforeCreate = erikoistuvaLaakariRepository.findAll().size

        // Create the ErikoistuvaLaakari
        val erikoistuvaLaakariDTO = erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)
        restErikoistuvaLaakariMockMvc.perform(
            post("/api/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoistuvaLaakariDTO))
        ).andExpect(status().isCreated)

        // Validate the ErikoistuvaLaakari in the database
        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeCreate + 1)
        val testErikoistuvaLaakari = erikoistuvaLaakariList[erikoistuvaLaakariList.size - 1]
        assertThat(testErikoistuvaLaakari.puhelinnumero).isEqualTo(DEFAULT_PUHELINNUMERO)
        assertThat(testErikoistuvaLaakari.sahkoposti).isEqualTo(DEFAULT_SAHKOPOSTI)
        assertThat(testErikoistuvaLaakari.opiskelijatunnus).isEqualTo(DEFAULT_OPISKELIJATUNNUS)
        assertThat(testErikoistuvaLaakari.opintojenAloitusvuosi).isEqualTo(DEFAULT_OPINTOJEN_ALOITUSVUOSI)
    }

    @Test
    @Transactional
    fun createErikoistuvaLaakariWithExistingId() {
        val databaseSizeBeforeCreate = erikoistuvaLaakariRepository.findAll().size

        // Create the ErikoistuvaLaakari with an existing ID
        erikoistuvaLaakari.id = 1L
        val erikoistuvaLaakariDTO = erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)

        // An entity with an existing ID cannot be created, so this API call must fail
        restErikoistuvaLaakariMockMvc.perform(
            post("/api/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoistuvaLaakariDTO))
        ).andExpect(status().isBadRequest)

        // Validate the ErikoistuvaLaakari in the database
        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkPuhelinnumeroIsRequired() {
        val databaseSizeBeforeTest = erikoistuvaLaakariRepository.findAll().size
        // set the field null
        erikoistuvaLaakari.puhelinnumero = null

        // Create the ErikoistuvaLaakari, which fails.
        val erikoistuvaLaakariDTO = erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)

        restErikoistuvaLaakariMockMvc.perform(
            post("/api/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoistuvaLaakariDTO))
        ).andExpect(status().isBadRequest)

        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkSahkopostiIsRequired() {
        val databaseSizeBeforeTest = erikoistuvaLaakariRepository.findAll().size
        // set the field null
        erikoistuvaLaakari.sahkoposti = null

        // Create the ErikoistuvaLaakari, which fails.
        val erikoistuvaLaakariDTO = erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)

        restErikoistuvaLaakariMockMvc.perform(
            post("/api/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoistuvaLaakariDTO))
        ).andExpect(status().isBadRequest)

        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllErikoistuvatLaakarit() {
        // Initialize the database
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        // Get all the erikoistuvaLaakariList
        restErikoistuvaLaakariMockMvc.perform(get("/api/erikoistuvat-laakarit?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(erikoistuvaLaakari.id?.toInt())))
            .andExpect(jsonPath("$.[*].puhelinnumero").value(hasItem(DEFAULT_PUHELINNUMERO)))
            .andExpect(jsonPath("$.[*].sahkoposti").value(hasItem(DEFAULT_SAHKOPOSTI)))
            .andExpect(jsonPath("$.[*].opiskelijatunnus").value(hasItem(DEFAULT_OPISKELIJATUNNUS)))
            .andExpect(jsonPath("$.[*].opintojenAloitusvuosi").value(hasItem(DEFAULT_OPINTOJEN_ALOITUSVUOSI))) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getErikoistuvaLaakari() {
        // Initialize the database
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        val id = erikoistuvaLaakari.id
        assertNotNull(id)

        // Get the erikoistuvaLaakari
        restErikoistuvaLaakariMockMvc.perform(get("/api/erikoistuvat-laakarit/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(erikoistuvaLaakari.id as Any))
            .andExpect(jsonPath("$.puhelinnumero").value(DEFAULT_PUHELINNUMERO))
            .andExpect(jsonPath("$.sahkoposti").value(DEFAULT_SAHKOPOSTI))
            .andExpect(jsonPath("$.opiskelijatunnus").value(DEFAULT_OPISKELIJATUNNUS))
            .andExpect(jsonPath("$.opintojenAloitusvuosi").value(DEFAULT_OPINTOJEN_ALOITUSVUOSI)) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingErikoistuvaLaakari() {
        // Get the erikoistuvaLaakari
        restErikoistuvaLaakariMockMvc.perform(get("/api/erikoistuvat-laakarit/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateErikoistuvaLaakari() {
        // Initialize the database
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        val databaseSizeBeforeUpdate = erikoistuvaLaakariRepository.findAll().size

        // Update the erikoistuvaLaakari
        val id = erikoistuvaLaakari.id
        assertNotNull(id)
        val updatedErikoistuvaLaakari = erikoistuvaLaakariRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedErikoistuvaLaakari are not directly saved in db
        em.detach(updatedErikoistuvaLaakari)
        updatedErikoistuvaLaakari.puhelinnumero = UPDATED_PUHELINNUMERO
        updatedErikoistuvaLaakari.sahkoposti = UPDATED_SAHKOPOSTI
        updatedErikoistuvaLaakari.opiskelijatunnus = UPDATED_OPISKELIJATUNNUS
        updatedErikoistuvaLaakari.opintojenAloitusvuosi = UPDATED_OPINTOJEN_ALOITUSVUOSI
        val erikoistuvaLaakariDTO = erikoistuvaLaakariMapper.toDto(updatedErikoistuvaLaakari)

        restErikoistuvaLaakariMockMvc.perform(
            put("/api/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoistuvaLaakariDTO))
        ).andExpect(status().isOk)

        // Validate the ErikoistuvaLaakari in the database
        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeUpdate)
        val testErikoistuvaLaakari = erikoistuvaLaakariList[erikoistuvaLaakariList.size - 1]
        assertThat(testErikoistuvaLaakari.puhelinnumero).isEqualTo(UPDATED_PUHELINNUMERO)
        assertThat(testErikoistuvaLaakari.sahkoposti).isEqualTo(UPDATED_SAHKOPOSTI)
        assertThat(testErikoistuvaLaakari.opiskelijatunnus).isEqualTo(UPDATED_OPISKELIJATUNNUS)
        assertThat(testErikoistuvaLaakari.opintojenAloitusvuosi).isEqualTo(UPDATED_OPINTOJEN_ALOITUSVUOSI)
    }

    @Test
    @Transactional
    fun updateNonExistingErikoistuvaLaakari() {
        val databaseSizeBeforeUpdate = erikoistuvaLaakariRepository.findAll().size

        // Create the ErikoistuvaLaakari
        val erikoistuvaLaakariDTO = erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restErikoistuvaLaakariMockMvc.perform(
            put("/api/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(erikoistuvaLaakariDTO))
        ).andExpect(status().isBadRequest)

        // Validate the ErikoistuvaLaakari in the database
        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteErikoistuvaLaakari() {
        // Initialize the database
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        val databaseSizeBeforeDelete = erikoistuvaLaakariRepository.findAll().size

        // Delete the erikoistuvaLaakari
        restErikoistuvaLaakariMockMvc.perform(
            delete("/api/erikoistuvat-laakarit/{id}", erikoistuvaLaakari.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val erikoistuvaLaakariList = erikoistuvaLaakariRepository.findAll()
        assertThat(erikoistuvaLaakariList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_PUHELINNUMERO = "AAAAAAAAAA"
        private const val UPDATED_PUHELINNUMERO = "BBBBBBBBBB"

        private const val DEFAULT_SAHKOPOSTI = "AAAAAAAAAA"
        private const val UPDATED_SAHKOPOSTI = "BBBBBBBBBB"

        private const val DEFAULT_OPISKELIJATUNNUS = "AAAAAAAAAA"
        private const val UPDATED_OPISKELIJATUNNUS = "BBBBBBBBBB"

        private const val DEFAULT_OPINTOJEN_ALOITUSVUOSI: Int = 1900
        private const val UPDATED_OPINTOJEN_ALOITUSVUOSI: Int = 1901

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari(
                puhelinnumero = DEFAULT_PUHELINNUMERO,
                sahkoposti = DEFAULT_SAHKOPOSTI,
                opiskelijatunnus = DEFAULT_OPISKELIJATUNNUS,
                opintojenAloitusvuosi = DEFAULT_OPINTOJEN_ALOITUSVUOSI
            )

            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaResourceIT.createEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            erikoistuvaLaakari.kayttaja = kayttaja
            return erikoistuvaLaakari
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): ErikoistuvaLaakari {
            val erikoistuvaLaakari = ErikoistuvaLaakari(
                puhelinnumero = UPDATED_PUHELINNUMERO,
                sahkoposti = UPDATED_SAHKOPOSTI,
                opiskelijatunnus = UPDATED_OPISKELIJATUNNUS,
                opintojenAloitusvuosi = UPDATED_OPINTOJEN_ALOITUSVUOSI
            )

            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaResourceIT.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            erikoistuvaLaakari.kayttaja = kayttaja
            return erikoistuvaLaakari
        }
    }
}
