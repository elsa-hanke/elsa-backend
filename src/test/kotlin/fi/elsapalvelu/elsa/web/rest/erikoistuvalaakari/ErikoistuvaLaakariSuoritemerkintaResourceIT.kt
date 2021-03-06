package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Oppimistavoite
import fi.elsapalvelu.elsa.domain.Suoritemerkinta
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.SuoritemerkintaMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.OppimistavoiteHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class ErikoistuvaLaakariSuoritemerkintaResourceIT {

    @Autowired
    private lateinit var suoritemerkintaRepository: SuoritemerkintaRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var suoritemerkintaMapper: SuoritemerkintaMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restSuoritemerkintaMockMvc: MockMvc

    private lateinit var suoritemerkinta: Suoritemerkinta

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun createSuoritemerkinta() {
        initTest()

        val databaseSizeBeforeCreate = suoritemerkintaRepository.findAll().size

        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(suoritemerkinta)
        restSuoritemerkintaMockMvc.perform(
            post("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeCreate + 1)
        val testSuoritemerkinta = suoritemerkintaList[suoritemerkintaList.size - 1]
        assertThat(testSuoritemerkinta.suorituspaiva).isEqualTo(DEFAULT_SUORITUSPAIVA)
        assertThat(testSuoritemerkinta.luottamuksenTaso).isEqualTo(DEFAULT_LUOTTAMUKSEN_TASO)
        assertThat(testSuoritemerkinta.vaativuustaso).isEqualTo(DEFAULT_VAATIVUUSTASO)
        assertThat(testSuoritemerkinta.lisatiedot).isEqualTo(DEFAULT_LISATIEDOT)
        assertThat(testSuoritemerkinta.lukittu).isEqualTo(DEFAULT_LUKITTU)
    }

    @Test
    @Transactional
    fun createSuoritemerkintaWithExistingId() {
        initTest()

        val databaseSizeBeforeCreate = suoritemerkintaRepository.findAll().size

        suoritemerkinta.id = 1L
        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(suoritemerkinta)
        restSuoritemerkintaMockMvc.perform(
            post("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createSuoritemerkintaForAnotherUser() {
        initTest(null)

        val databaseSizeBeforeCreate = suoritemerkintaRepository.findAll().size

        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(suoritemerkinta)
        restSuoritemerkintaMockMvc.perform(
            post("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun getSuoritemerkinta() {
        initTest()

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val id = suoritemerkinta.id
        assertNotNull(id)

        restSuoritemerkintaMockMvc.perform(get("/api/erikoistuva-laakari/suoritemerkinnat/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(suoritemerkinta.id as Any))
            .andExpect(jsonPath("$.suorituspaiva").value(DEFAULT_SUORITUSPAIVA.toString()))
            .andExpect(jsonPath("$.luottamuksenTaso").value(DEFAULT_LUOTTAMUKSEN_TASO))
            .andExpect(jsonPath("$.vaativuustaso").value(DEFAULT_VAATIVUUSTASO))
            .andExpect(jsonPath("$.lisatiedot").value(DEFAULT_LISATIEDOT))
            .andExpect(jsonPath("$.lukittu").value(DEFAULT_LUKITTU))
    }

    @Test
    @Transactional
    fun getAnotherUserSuoritemerkinta() {
        initTest(null)

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val id = suoritemerkinta.id
        assertNotNull(id)

        restSuoritemerkintaMockMvc.perform(get("/api/erikoistuva-laakari/suoritemerkinnat/{id}", id))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateSuoritemerkinta() {
        initTest()

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val databaseSizeBeforeUpdate = suoritemerkintaRepository.findAll().size

        // Päivitetään suoritemerkintä
        val id = suoritemerkinta.id
        assertNotNull(id)
        val updatedSuoritemerkinta = suoritemerkintaRepository.findById(id).get()
        em.detach(updatedSuoritemerkinta)

        updatedSuoritemerkinta.suorituspaiva = UPDATED_SUORITUSPAIVA
        updatedSuoritemerkinta.luottamuksenTaso = UPDATED_LUOTTAMUKSEN_TASO
        updatedSuoritemerkinta.vaativuustaso = UPDATED_VAATIVUUSTASO
        updatedSuoritemerkinta.lisatiedot = UPDATED_LISATIEDOT
        updatedSuoritemerkinta.lukittu = UPDATED_LUKITTU
        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(updatedSuoritemerkinta)

        restSuoritemerkintaMockMvc.perform(
            put("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeUpdate)
        val testSuoritemerkinta = suoritemerkintaList[suoritemerkintaList.size - 1]
        assertThat(testSuoritemerkinta.suorituspaiva).isEqualTo(UPDATED_SUORITUSPAIVA)
        assertThat(testSuoritemerkinta.luottamuksenTaso).isEqualTo(UPDATED_LUOTTAMUKSEN_TASO)
        assertThat(testSuoritemerkinta.vaativuustaso).isEqualTo(UPDATED_VAATIVUUSTASO)
        assertThat(testSuoritemerkinta.lisatiedot).isEqualTo(UPDATED_LISATIEDOT)
        assertThat(testSuoritemerkinta.lukittu).isEqualTo(false) // Lukitseminen tehdään eri rajapinnan kautta
    }

    @Test
    @Transactional
    fun updateNonExistingSuoritemerkinta() {
        initTest()

        val databaseSizeBeforeUpdate = suoritemerkintaRepository.findAll().size

        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(suoritemerkinta)
        restSuoritemerkintaMockMvc.perform(
            put("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateAnotherUserSuoritemerkinta() {
        initTest(null)

        val databaseSizeBeforeCreate = suoritemerkintaRepository.findAll().size

        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(suoritemerkinta)
        restSuoritemerkintaMockMvc.perform(
            post("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun updateLukittuSuoritemerkinta() {
        initTest()

        suoritemerkinta.lukittu = true
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val databaseSizeBeforeUpdate = suoritemerkintaRepository.findAll().size

        // Päivitetään suoritemerkintä
        val id = suoritemerkinta.id
        assertNotNull(id)
        val updatedSuoritemerkinta = suoritemerkintaRepository.findById(id).get()
        em.detach(updatedSuoritemerkinta)

        updatedSuoritemerkinta.suorituspaiva = UPDATED_SUORITUSPAIVA
        updatedSuoritemerkinta.luottamuksenTaso = UPDATED_LUOTTAMUKSEN_TASO
        updatedSuoritemerkinta.vaativuustaso = UPDATED_VAATIVUUSTASO
        updatedSuoritemerkinta.lisatiedot = UPDATED_LISATIEDOT
        updatedSuoritemerkinta.lukittu = UPDATED_LUKITTU
        val suoritemerkintaDTO = suoritemerkintaMapper.toDto(updatedSuoritemerkinta)

        restSuoritemerkintaMockMvc.perform(
            put("/api/erikoistuva-laakari/suoritemerkinnat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(suoritemerkintaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeUpdate)
        val testSuoritemerkinta = suoritemerkintaList[suoritemerkintaList.size - 1]
        assertThat(testSuoritemerkinta.suorituspaiva).isEqualTo(DEFAULT_SUORITUSPAIVA)
        assertThat(testSuoritemerkinta.luottamuksenTaso).isEqualTo(DEFAULT_LUOTTAMUKSEN_TASO)
        assertThat(testSuoritemerkinta.vaativuustaso).isEqualTo(DEFAULT_VAATIVUUSTASO)
        assertThat(testSuoritemerkinta.lisatiedot).isEqualTo(DEFAULT_LISATIEDOT)
        assertThat(testSuoritemerkinta.lukittu).isEqualTo(true)
    }

    @Test
    @Transactional
    fun deleteSuoritemerkinta() {
        initTest()

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val databaseSizeBeforeDelete = suoritemerkintaRepository.findAll().size

        restSuoritemerkintaMockMvc.perform(
            delete("/api/erikoistuva-laakari/suoritemerkinnat/{id}", suoritemerkinta.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteAnotherUserSuoritemerkinta() {
        initTest(null)

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val databaseSizeBeforeDelete = suoritemerkintaRepository.findAll().size

        restSuoritemerkintaMockMvc.perform(
            delete("/api/erikoistuva-laakari/suoritemerkinnat/{id}", suoritemerkinta.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun deleteLukittuSuoritemerkinta() {
        initTest()

        suoritemerkinta.lukittu = true
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val databaseSizeBeforeDelete = suoritemerkintaRepository.findAll().size

        restSuoritemerkintaMockMvc.perform(
            delete("/api/erikoistuva-laakari/suoritemerkinnat/{id}", suoritemerkinta.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val suoritemerkintaList = suoritemerkintaRepository.findAll()
        assertThat(suoritemerkintaList).hasSize(databaseSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun getOppimistavoitteetTable() {
        initTest()

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val id = suoritemerkinta.id
        assertNotNull(id)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(DEFAULT_ID)!!
        erikoistuvaLaakari.erikoisala = suoritemerkinta.oppimistavoite?.kategoria?.erikoisala
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        restSuoritemerkintaMockMvc.perform(get("/api/erikoistuva-laakari/oppimistavoitteet-taulukko"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.suoritemerkinnat").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.suoritemerkinnat[0].id").value(suoritemerkinta.id as Any))
            .andExpect(jsonPath("$.oppimistavoitteenKategoriat").value(Matchers.hasSize<Any>(1)))
            .andExpect(
                jsonPath("$.oppimistavoitteenKategoriat[0].id")
                    .value(suoritemerkinta.oppimistavoite?.kategoria?.id as Any)
            )
    }

    @Test
    @Transactional
    fun getSuoritemerkintaForm() {
        initTest()

        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)

        val id = suoritemerkinta.id
        assertNotNull(id)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(DEFAULT_ID)!!
        erikoistuvaLaakari.erikoisala = suoritemerkinta.oppimistavoite?.kategoria?.erikoisala
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        restSuoritemerkintaMockMvc.perform(get("/api/erikoistuva-laakari/suoritemerkinta-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.tyoskentelyjaksot[0].id").value(suoritemerkinta.tyoskentelyjakso?.id as Any))
            .andExpect(
                jsonPath("$.oppimistavoitteenKategoriat[0].id")
                    .value(suoritemerkinta.oppimistavoite?.kategoria?.id as Any)
            )
    }

    fun initTest(userId: String? = DEFAULT_ID) {
        val userDetails = mapOf<String, Any>(
            "uid" to DEFAULT_ID,
            "sub" to DEFAULT_LOGIN,
            "email" to DEFAULT_EMAIL
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val user = DefaultOAuth2User(authorities, userDetails, "sub")
        val authentication = OAuth2AuthenticationToken(user, authorities, "oidc")
        TestSecurityContextHolder.getContext().authentication = authentication

        suoritemerkinta = createEntity(em, userId)
    }

    companion object {

        private const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        private const val DEFAULT_LOGIN = "johndoe"
        private const val DEFAULT_EMAIL = "john.doe@example.com"

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
        fun createEntity(em: EntityManager, userId: String? = null): Suoritemerkinta {
            val suoritemerkinta = Suoritemerkinta(
                suorituspaiva = DEFAULT_SUORITUSPAIVA,
                luottamuksenTaso = DEFAULT_LUOTTAMUKSEN_TASO,
                vaativuustaso = DEFAULT_VAATIVUUSTASO,
                lisatiedot = DEFAULT_LISATIEDOT,
                lukittu = DEFAULT_LUKITTU
            )

            // Lisätään pakollinen tieto
            val oppimistavoite: Oppimistavoite
            if (em.findAll(Oppimistavoite::class).isEmpty()) {
                oppimistavoite = OppimistavoiteHelper.createEntity(em)
                em.persist(oppimistavoite)
                em.flush()
            } else {
                oppimistavoite = em.findAll(Oppimistavoite::class)[0]
            }
            suoritemerkinta.oppimistavoite = oppimistavoite

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, userId)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class)[0]
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

            // Lisätään pakollinen tieto
            val oppimistavoite: Oppimistavoite
            if (em.findAll(Oppimistavoite::class).isEmpty()) {
                oppimistavoite = OppimistavoiteHelper.createUpdatedEntity(em)
                em.persist(oppimistavoite)
                em.flush()
            } else {
                oppimistavoite = em.findAll(Oppimistavoite::class)[0]
            }
            suoritemerkinta.oppimistavoite = oppimistavoite

            // Lisätään pakollinen tieto
            val tyoskentelyjakso: Tyoskentelyjakso
            if (em.findAll(Tyoskentelyjakso::class).isEmpty()) {
                tyoskentelyjakso = TyoskentelyjaksoHelper.createUpdatedEntity(em)
                em.persist(tyoskentelyjakso)
                em.flush()
            } else {
                tyoskentelyjakso = em.findAll(Tyoskentelyjakso::class)[0]
            }
            suoritemerkinta.tyoskentelyjakso = tyoskentelyjakso
            return suoritemerkinta
        }
    }
}
