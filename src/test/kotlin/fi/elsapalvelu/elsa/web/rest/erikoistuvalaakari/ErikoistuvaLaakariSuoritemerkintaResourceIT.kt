package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.mapper.SuoritemerkintaMapper
import fi.elsapalvelu.elsa.web.rest.crud.OppimistavoiteResourceIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.createFormattingConversionService
import fi.elsapalvelu.elsa.web.rest.crud.TyoskentelyjaksoResourceIT
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
import fi.elsapalvelu.elsa.web.rest.findAll
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions
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

@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class ErikoistuvaLaakariSuoritemerkintaResourceIT {

    @Autowired
    private lateinit var suoritemerkintaRepository: SuoritemerkintaRepository

    @Autowired
    private lateinit var suoritemerkintaMapper: SuoritemerkintaMapper

    @Autowired
    private lateinit var suoritemerkintaService: SuoritemerkintaService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var tyoskentelyjaksoService: TyoskentelyjaksoService

    @Autowired
    private lateinit var oppimistavoitteenKategoriaService: OppimistavoitteenKategoriaService

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

    private lateinit var restSuoritemerkintaMockMvc: MockMvc

    private lateinit var suoritemerkinta: Suoritemerkinta

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val erikoistuvaLaakariSuoritemerkintaResource = ErikoistuvaLaakariSuoritemerkintaResource(
            userService,
            tyoskentelyjaksoService,
            oppimistavoitteenKategoriaService,
            suoritemerkintaService,
            erikoistuvaLaakariService
        )
        this.restSuoritemerkintaMockMvc = MockMvcBuilders.standaloneSetup(erikoistuvaLaakariSuoritemerkintaResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build()
    }

    @BeforeEach
    fun initTest() {
        suoritemerkinta = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createSuoritemerkinta() {
        val databaseSizeBeforeCreate = suoritemerkintaRepository.findAll().size

        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(suoritemerkinta)
        restSuoritemerkintaMockMvc.perform(
            post("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
        ).andExpect(status().isCreated)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        Assertions.assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeCreate + 1)
        val testSuoritemerkinta = suoritemerkintaList[suoritemerkintaList.size - 1]
        assertThat(testSuoritemerkinta.suorituspaiva).isEqualTo(DEFAULT_SUORITUSPAIVA)
        assertThat(testSuoritemerkinta.luottamuksenTaso).isEqualTo(DEFAULT_LUOTTAMUKSEN_TASO)
        assertThat(testSuoritemerkinta.vaativuustaso).isEqualTo(DEFAULT_VAATIVUUSTASO)
        assertThat(testSuoritemerkinta.lisatiedot).isEqualTo(DEFAULT_LISATIEDOT)
        assertThat(testSuoritemerkinta.lukittu).isEqualTo(DEFAULT_LUKITTU)
    }

    // TODO: testi uuden suoritemerkinnän luonnille
    // TODO: suoritemerkinnnän muokkaamiselle
    // TODO: suoritemerkinnnän muokkaamiselle kun lukittu -> feilaus
    // TODO: suoritemerkinnnän poistaminen
    // TODO: suoritemerkinnnän poistaminen kun lukittu -> feilaus
    // TODO: suoritemerkinnän lomakkeen tiedot
    // TODO: oppimistavoitteet taulukon tiedot

    companion object {

        private val DEFAULT_SUORITUSPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_SUORITUSPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LUOTTAMUKSEN_TASO: Int = 1
        private const val UPDATED_LUOTTAMUKSEN_TASO: Int = 2

        private const val DEFAULT_VAATIVUUSTASO: Int = 1
        private const val UPDATED_VAATIVUUSTASO: Int = 2

        private const val DEFAULT_LISATIEDOT = "AAAAAAAAAA"
        private const val UPDATED_LISATIEDOT = "BBBBBBBBBB"

        private const val DEFAULT_LUKITTU: Boolean = false
        private const val UPDATED_LUKITTU: Boolean = true

        @JvmStatic
        fun createEntity(em: EntityManager): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = DEFAULT_SUORITUSPAIVA,
                luottamuksenTaso = DEFAULT_LUOTTAMUKSEN_TASO,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                lisatiedot = DEFAULT_LISATIEDOT,
                lukittu = DEFAULT_LUKITTU
            )

            // Add required entity
            val oppimistavoite: Oppimistavoite
            if (em.findAll(Oppimistavoite::class).isEmpty()) {
                oppimistavoite = OppimistavoiteResourceIT.createEntity(em)
                em.persist(oppimistavoite)
                em.flush()
            } else {
                oppimistavoite = em.findAll(Oppimistavoite::class).get(0)
            }
            suoritemerkinta.oppimistavoite = oppimistavoite
            // Add required entity
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoResourceIT.createEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso
            return suoritemerkinta
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = UPDATED_SUORITUSPAIVA,
                luottamuksenTaso = UPDATED_LUOTTAMUKSEN_TASO,
                vaativuustaso = UPDATED_VAATIVUUSTASO,
                lisatiedot = UPDATED_LISATIEDOT,
                lukittu = UPDATED_LUKITTU
            )

            // Add required entity
            val oppimistavoite: Oppimistavoite
            if (em.findAll(Oppimistavoite::class).isEmpty()) {
                oppimistavoite = OppimistavoiteResourceIT.createUpdatedEntity(em)
                em.persist(oppimistavoite)
                em.flush()
            } else {
                oppimistavoite = em.findAll(Oppimistavoite::class).get(0)
            }
            suoritemerkinta.oppimistavoite = oppimistavoite
            // Add required entity
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoResourceIT.createUpdatedEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class).get(0)
            }
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso
            return suoritemerkinta
        }
    }
}
