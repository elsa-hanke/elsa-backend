package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.mapper.KouluttajavaltuutusMapper
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
 * Integration tests for the [KouluttajavaltuutusResource] REST controller.
 *
 * @see KouluttajavaltuutusResource
 */
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class KouluttajavaltuutusResourceIT {

    @Autowired
    private lateinit var kouluttajavaltuutusRepository: KouluttajavaltuutusRepository

    @Autowired
    private lateinit var kouluttajavaltuutusMapper: KouluttajavaltuutusMapper

    @Autowired
    private lateinit var kouluttajavaltuutusService: KouluttajavaltuutusService

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

    private lateinit var restKouluttajavaltuutusMockMvc: MockMvc

    private lateinit var kouluttajavaltuutus: Kouluttajavaltuutus

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val kouluttajavaltuutusResource = KouluttajavaltuutusResource(kouluttajavaltuutusService)
        this.restKouluttajavaltuutusMockMvc = MockMvcBuilders.standaloneSetup(kouluttajavaltuutusResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        kouluttajavaltuutus = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createKouluttajavaltuutus() {
        val databaseSizeBeforeCreate = kouluttajavaltuutusRepository.findAll().size

        // Create the Kouluttajavaltuutus
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)
        restKouluttajavaltuutusMockMvc.perform(
            post("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isCreated)

        // Validate the Kouluttajavaltuutus in the database
        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeCreate + 1)
        val testKouluttajavaltuutus = kouluttajavaltuutusList[kouluttajavaltuutusList.size - 1]
        assertThat(testKouluttajavaltuutus.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testKouluttajavaltuutus.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testKouluttajavaltuutus.valtuutuksenLuontiaika).isEqualTo(DEFAULT_VALTUUTUKSEN_LUONTIAIKA)
        assertThat(testKouluttajavaltuutus.valtuutuksenMuokkausaika).isEqualTo(DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA)
    }

    @Test
    @Transactional
    fun createKouluttajavaltuutusWithExistingId() {
        val databaseSizeBeforeCreate = kouluttajavaltuutusRepository.findAll().size

        // Create the Kouluttajavaltuutus with an existing ID
        kouluttajavaltuutus.id = 1L
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)

        // An entity with an existing ID cannot be created, so this API call must fail
        restKouluttajavaltuutusMockMvc.perform(
            post("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Kouluttajavaltuutus in the database
        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkAlkamispaivaIsRequired() {
        val databaseSizeBeforeTest = kouluttajavaltuutusRepository.findAll().size
        // set the field null
        kouluttajavaltuutus.alkamispaiva = null

        // Create the Kouluttajavaltuutus, which fails.
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            post("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isBadRequest)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkPaattymispaivaIsRequired() {
        val databaseSizeBeforeTest = kouluttajavaltuutusRepository.findAll().size
        // set the field null
        kouluttajavaltuutus.paattymispaiva = null

        // Create the Kouluttajavaltuutus, which fails.
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            post("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isBadRequest)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkValtuutuksenLuontiaikaIsRequired() {
        val databaseSizeBeforeTest = kouluttajavaltuutusRepository.findAll().size
        // set the field null
        kouluttajavaltuutus.valtuutuksenLuontiaika = null

        // Create the Kouluttajavaltuutus, which fails.
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            post("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isBadRequest)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun checkValtuutuksenMuokkausaikaIsRequired() {
        val databaseSizeBeforeTest = kouluttajavaltuutusRepository.findAll().size
        // set the field null
        kouluttajavaltuutus.valtuutuksenMuokkausaika = null

        // Create the Kouluttajavaltuutus, which fails.
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            post("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isBadRequest)

        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllKouluttajavaltuutukset() {
        // Initialize the database
        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        // Get all the kouluttajavaltuutusList
        restKouluttajavaltuutusMockMvc.perform(get("/api/kouluttajavaltuutukset?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id")
                .value(hasItem(kouluttajavaltuutus.id?.toInt())))
            .andExpect(jsonPath("$.[*].alkamispaiva")
                .value(hasItem(DEFAULT_ALKAMISPAIVA.toString())))
            .andExpect(jsonPath("$.[*].paattymispaiva")
                .value(hasItem(DEFAULT_PAATTYMISPAIVA.toString())))
            .andExpect(jsonPath("$.[*].valtuutuksenLuontiaika")
                .value(hasItem(DEFAULT_VALTUUTUKSEN_LUONTIAIKA.toString())))
            .andExpect(jsonPath("$.[*].valtuutuksenMuokkausaika")
                .value(hasItem(DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA.toString()))) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getKouluttajavaltuutus() {
        // Initialize the database
        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        val id = kouluttajavaltuutus.id
        assertNotNull(id)

        // Get the kouluttajavaltuutus
        restKouluttajavaltuutusMockMvc.perform(get("/api/kouluttajavaltuutukset/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id")
                .value(kouluttajavaltuutus.id?.toInt()))
            .andExpect(jsonPath("$.alkamispaiva")
                .value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva")
                .value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.valtuutuksenLuontiaika")
                .value(DEFAULT_VALTUUTUKSEN_LUONTIAIKA.toString()))
            .andExpect(jsonPath("$.valtuutuksenMuokkausaika")
                .value(DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA.toString())) }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingKouluttajavaltuutus() {
        // Get the kouluttajavaltuutus
        restKouluttajavaltuutusMockMvc.perform(get("/api/kouluttajavaltuutukset/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun updateKouluttajavaltuutus() {
        // Initialize the database
        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        val databaseSizeBeforeUpdate = kouluttajavaltuutusRepository.findAll().size

        // Update the kouluttajavaltuutus
        val id = kouluttajavaltuutus.id
        assertNotNull(id)
        val updatedKouluttajavaltuutus = kouluttajavaltuutusRepository.findById(id).get()
        // Disconnect from session so that the updates on updatedKouluttajavaltuutus are not directly saved in db
        em.detach(updatedKouluttajavaltuutus)
        updatedKouluttajavaltuutus.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedKouluttajavaltuutus.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedKouluttajavaltuutus.valtuutuksenLuontiaika = UPDATED_VALTUUTUKSEN_LUONTIAIKA
        updatedKouluttajavaltuutus.valtuutuksenMuokkausaika = UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(updatedKouluttajavaltuutus)

        restKouluttajavaltuutusMockMvc.perform(
            put("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isOk)

        // Validate the Kouluttajavaltuutus in the database
        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeUpdate)
        val testKouluttajavaltuutus = kouluttajavaltuutusList[kouluttajavaltuutusList.size - 1]
        assertThat(testKouluttajavaltuutus.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testKouluttajavaltuutus.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testKouluttajavaltuutus.valtuutuksenLuontiaika).isEqualTo(UPDATED_VALTUUTUKSEN_LUONTIAIKA)
        assertThat(testKouluttajavaltuutus.valtuutuksenMuokkausaika).isEqualTo(UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA)
    }

    @Test
    @Transactional
    fun updateNonExistingKouluttajavaltuutus() {
        val databaseSizeBeforeUpdate = kouluttajavaltuutusRepository.findAll().size

        // Create the Kouluttajavaltuutus
        val kouluttajavaltuutusDTO = kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restKouluttajavaltuutusMockMvc.perform(
            put("/api/kouluttajavaltuutukset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kouluttajavaltuutusDTO))
        ).andExpect(status().isBadRequest)

        // Validate the Kouluttajavaltuutus in the database
        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteKouluttajavaltuutus() {
        // Initialize the database
        kouluttajavaltuutusRepository.saveAndFlush(kouluttajavaltuutus)

        val databaseSizeBeforeDelete = kouluttajavaltuutusRepository.findAll().size

        // Delete the kouluttajavaltuutus
        restKouluttajavaltuutusMockMvc.perform(
            delete("/api/kouluttajavaltuutukset/{id}", kouluttajavaltuutus.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val kouluttajavaltuutusList = kouluttajavaltuutusRepository.findAll()
        assertThat(kouluttajavaltuutusList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VALTUUTUKSEN_LUONTIAIKA: Instant = Instant.ofEpochMilli(0L)
        private val UPDATED_VALTUUTUKSEN_LUONTIAIKA: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        private val DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA: Instant = Instant.ofEpochMilli(0L)
        private val UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Kouluttajavaltuutus {
            val kouluttajavaltuutus = Kouluttajavaltuutus(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                valtuutuksenLuontiaika = DEFAULT_VALTUUTUKSEN_LUONTIAIKA,
                valtuutuksenMuokkausaika = DEFAULT_VALTUUTUKSEN_MUOKKAUSAIKA
            )

            // Add required entity
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariResourceIT.createEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            kouluttajavaltuutus.valtuuttaja = erikoistuvaLaakari
            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaResourceIT.createEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            kouluttajavaltuutus.valtuutettu = kayttaja
            return kouluttajavaltuutus
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Kouluttajavaltuutus {
            val kouluttajavaltuutus = Kouluttajavaltuutus(
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                valtuutuksenLuontiaika = UPDATED_VALTUUTUKSEN_LUONTIAIKA,
                valtuutuksenMuokkausaika = UPDATED_VALTUUTUKSEN_MUOKKAUSAIKA
            )

            // Add required entity
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariResourceIT.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            kouluttajavaltuutus.valtuuttaja = erikoistuvaLaakari
            // Add required entity
            val kayttaja: Kayttaja
            if (em.findAll(Kayttaja::class).isEmpty()) {
                kayttaja = KayttajaResourceIT.createUpdatedEntity(em)
                em.persist(kayttaja)
                em.flush()
            } else {
                kayttaja = em.findAll(Kayttaja::class).get(0)
            }
            kouluttajavaltuutus.valtuutettu = kayttaja
            return kouluttajavaltuutus
        }
    }
}
