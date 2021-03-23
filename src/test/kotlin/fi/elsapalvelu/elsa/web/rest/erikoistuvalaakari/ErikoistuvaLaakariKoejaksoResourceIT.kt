package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KoejaksoRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import org.assertj.core.api.Assertions.assertThat
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
class ErikoistuvaLaakariKoejaksoResourceIT {

    @Autowired
    private lateinit var koejaksoRepository: KoejaksoRepository

    @Autowired
    private lateinit var koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository

    @Autowired
    private lateinit var koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejakso: Koejakso

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus

    private lateinit var koulutussopimuksenKouluttajat: MutableSet<KoulutussopimuksenKouluttaja>

    private lateinit var koulutussopimuksenKoulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikka>

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun getKoejakso() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)

        val id = koejakso.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/koejakso"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejakso.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").isEmpty)
            .andExpect(jsonPath("$.paattymispaiva").isEmpty)
            .andExpect(jsonPath("$.koulutussopimus").isEmpty)
            .andExpect(jsonPath("$.tyoskentelyjaksot").isEmpty)
    }

    @Test
    @Transactional
    fun addTyoskentelyjakso() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)

        var tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, DEFAULT_ID)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        assertThat(koejakso.tyoskentelyjaksot).hasSize(0)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/tyoskentelyjaksot/" + tyoskentelyjakso.id)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isOk)

        tyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        assertThat(tyoskentelyjakso.koejakso?.id).isEqualTo(koejakso.id)
    }

    @Test
    @Transactional
    fun removeTyoskentelyjakso() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)

        var tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, DEFAULT_ID)
        tyoskentelyjakso.koejakso = koejakso
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        restKoejaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/tyoskentelyjaksot/" + tyoskentelyjakso.id)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        tyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id!!).get()
        assertThat(tyoskentelyjakso.koejakso).isNull()
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithExistingId() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)
        koejakso.koulutussopimus = koejaksonKoulutussopimus
        koejaksonKoulutussopimus.kouluttajat = koulutussopimuksenKouluttajat
        koejaksonKoulutussopimus.koulutuspaikat = koulutussopimuksenKoulutuspaikat
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejakso.koulutussopimus!!)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val sopimus = koejaksonKoulutussopimusRepository.findById(koejakso.koulutussopimus?.id!!)
        assertThat(sopimus.get().muokkauspaiva).isEqualTo(koejakso.koulutussopimus?.muokkauspaiva)
    }

    @Test
    @Transactional
    fun createKoulutussopimusForAnotherUser() {
        initTest(null)

        koejaksoRepository.saveAndFlush(koejakso)

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithKouluttajaAck() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)

        val kouluttaja = koulutussopimuksenKouluttajat.iterator().next()
        kouluttaja.sopimusHyvaksytty = true
        kouluttaja.kuittausaika = LocalDate.now()
        koejaksonKoulutussopimus.kouluttajat = koulutussopimuksenKouluttajat

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithVastuuhenkiloAck() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)

        koejaksonKoulutussopimus.vastuuhenkilonKuittausaika = LocalDate.now()

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKoulutussopimus() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)
        koejaksonKoulutussopimus.kouluttajat = koulutussopimuksenKouluttajat
        koejaksonKoulutussopimus.koulutuspaikat = koulutussopimuksenKoulutuspaikat

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate + 1)
        val sopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(sopimus.erikoistuvanNimi).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanNimi)
        assertThat(sopimus.erikoistuvanOpiskelijatunnus).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanOpiskelijatunnus)
        assertThat(sopimus.erikoistuvanSyntymaaika).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanSyntymaaika)
        assertThat(sopimus.erikoistuvanYliopisto).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanYliopisto)
        assertThat(sopimus.erikoistuvanPuhelinnumero).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero)
        assertThat(sopimus.erikoistuvanSahkoposti).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti)
        assertThat(sopimus.opintooikeudenMyontamispaiva).isEqualTo(koejaksonKoulutussopimusDTO.opintooikeudenMyontamispaiva)
        assertThat(sopimus.koejaksonAlkamispaiva).isEqualTo(koejaksonKoulutussopimusDTO.koejaksonAlkamispaiva)
        assertThat(sopimus.lahetetty).isEqualTo(koejaksonKoulutussopimusDTO.lahetetty)
        assertThat(sopimus.muokkauspaiva).isNotNull
        assertThat(sopimus.vastuuhenkilo?.id).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.id)
        assertThat(sopimus.vastuuhenkilonNimi).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.nimi)
        assertThat(sopimus.vastuuhenkilonNimike).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.nimike)
        assertThat(sopimus.vastuuhenkiloHyvaksynyt).isFalse
        assertThat(sopimus.vastuuhenkilonKuittausaika).isNull()
        assertThat(sopimus.korjausehdotus).isNull()
        assertThat(sopimus.kouluttajat).hasSize(1)
        val kouluttaja = sopimus.kouluttajat.iterator().next()
        val kouluttajaDTO = koulutussopimuksenKouluttajat.iterator().next()
        assertThat(kouluttaja.nimi).isEqualTo(kouluttajaDTO.nimi)
        assertThat(kouluttaja.nimike).isEqualTo(kouluttajaDTO.nimike)
        assertThat(kouluttaja.kouluttaja?.id).isEqualTo(kouluttajaDTO.kouluttaja?.id)
        assertThat(kouluttaja.toimipaikka).isEqualTo(kouluttajaDTO.toimipaikka)
        assertThat(kouluttaja.lahiosoite).isEqualTo(kouluttajaDTO.lahiosoite)
        assertThat(kouluttaja.postitoimipaikka).isEqualTo(kouluttajaDTO.postitoimipaikka)
        assertThat(kouluttaja.puhelin).isEqualTo(kouluttajaDTO.puhelin)
        assertThat(kouluttaja.sahkoposti).isEqualTo(kouluttajaDTO.sahkoposti)
        assertThat(kouluttaja.sopimusHyvaksytty).isFalse
        assertThat(kouluttaja.kuittausaika).isNull()
        assertThat(sopimus.koulutuspaikat).hasSize(1)
        val koulutuspaikka = sopimus.koulutuspaikat.iterator().next()
        val koulutuspaikkaDTO = koulutussopimuksenKoulutuspaikat.iterator().next()
        assertThat(koulutuspaikka.nimi).isEqualTo(koulutuspaikkaDTO.nimi)
        assertThat(koulutuspaikka.yliopisto).isEqualTo(koulutuspaikkaDTO.yliopisto)
    }

    @Test
    @Transactional
    fun updateKoulutussopimus() {
        initTest()

        koejaksoRepository.saveAndFlush(koejakso)
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)
        koejakso.koulutussopimus = koejaksonKoulutussopimus
        em.persist(koulutussopimuksenKouluttajat.iterator().next())
        koejaksonKoulutussopimus.kouluttajat = koulutussopimuksenKouluttajat
        em.persist(koulutussopimuksenKoulutuspaikat.iterator().next())
        koejaksonKoulutussopimus.koulutuspaikat = koulutussopimuksenKoulutuspaikat
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.koejaksonAlkamispaiva = UPDATED_ALKAMISPAIVA
        updatedKoulutussopimus.erikoistuvanSahkoposti = UPDATED_EMAIL
        updatedKoulutussopimus.erikoistuvanPuhelinnumero = UPDATED_PHONE

        val updatedKoulutuspaikka = updatedKoulutussopimus.koulutuspaikat.iterator().next()
        updatedKoulutuspaikka.nimi = UPDATED_KOULUTUSPAIKKA

        val updatedKouluttaja =
            createKoulutussopimuksenKouluttaja(em, updatedKoulutussopimus, UPDATED_KOULUTTAJA_ID)
        em.persist(updatedKouluttaja)
        updatedKoulutussopimus.kouluttajat.add(updatedKouluttaja)

        val updatedVastuuhenkilo = KayttajaHelper.createUpdatedEntity(
            em,
            UPDATED_VASTUUHENKILO_ID,
            UPDATED_VASTUUHENKILO_NIMI
        )
        em.persist(updatedVastuuhenkilo)
        updatedKoulutussopimus.vastuuhenkilo = updatedVastuuhenkilo
        updatedKoulutussopimus.vastuuhenkilonNimi = updatedVastuuhenkilo.nimi
        updatedKoulutussopimus.vastuuhenkilonNimike = updatedVastuuhenkilo.nimike

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/" + koejakso.id + "/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.koejaksonAlkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testKoulutussopimus.erikoistuvanSahkoposti).isEqualTo(UPDATED_EMAIL)
        assertThat(testKoulutussopimus.erikoistuvanPuhelinnumero).isEqualTo(UPDATED_PHONE)

        val testKoulutuspaikka = testKoulutussopimus.koulutuspaikat.iterator().next()
        assertThat(testKoulutuspaikka.nimi).isEqualTo(UPDATED_KOULUTUSPAIKKA)

        assertThat(testKoulutussopimus.kouluttajat).hasSize(2)
        val testKouluttaja = testKoulutussopimus.kouluttajat.last()
        assertThat(testKouluttaja.kouluttaja?.user?.id).isEqualTo(UPDATED_KOULUTTAJA_ID)

        assertThat(testKoulutussopimus.vastuuhenkilo?.user?.id).isEqualTo(UPDATED_VASTUUHENKILO_ID)
        assertThat(testKoulutussopimus.vastuuhenkilonNimi).isEqualTo(UPDATED_VASTUUHENKILO_NIMI)
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
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, userId)
        em.persist(erikoistuvaLaakari)

        koejakso = erikoistuvaLaakari.koejakso!!
        koejaksonKoulutussopimus = createKoulutussopimus(em, erikoistuvaLaakari)
        koulutussopimuksenKouluttajat =
            mutableSetOf(createKoulutussopimuksenKouluttaja(em, koejaksonKoulutussopimus))
        koulutussopimuksenKoulutuspaikat =
            mutableSetOf(createKoulutussopimuksenKoulutuspaikka(koejaksonKoulutussopimus))
    }

    companion object {

        private const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        private const val DEFAULT_LOGIN = "johndoe"
        private const val DEFAULT_EMAIL = "john.doe@example.com"

        private const val UPDATED_EMAIL = "doe.john@example.com"
        private const val UPDATED_PHONE = "+358101001010"

        private val DEFAULT_SYNTYMAAIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val DEFAULT_MYONTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(1L)
        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(2L)
        private val DEFAULT_MUOKKAUSPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(3L)

        private const val DEFAULT_KOULUTTAJA_ID = "4b73bc2c-88c4-11eb-8dcd-0242ac130003"
        private const val DEFAULT_VASTUUHENKILO_ID = "53d6e70e-88c4-11eb-8dcd-0242ac130003"

        private const val UPDATED_KOULUTTAJA_ID = "914cb8c5-c56d-4ab4-81a8-e51b5db0a85b"
        private const val UPDATED_VASTUUHENKILO_ID = "1df48f72-8bbe-11eb-8dcd-0242ac130003"
        private const val UPDATED_VASTUUHENKILO_NIMI = "Ville Vastuuhenkilö"

        private const val DEFAULT_KOULUTUSPAIKKA = "TAYS Päivystyskeskus"
        private const val DEFAULT_YLIOPISTO = "TAYS"

        private const val UPDATED_KOULUTUSPAIKKA = "HUS Päivystyskeskus"

        @JvmStatic
        fun createKoulutussopimus(
            em: EntityManager,
            erikoistuvaLaakari: ErikoistuvaLaakari
        ): KoejaksonKoulutussopimus {
            val vastuuhenkilo = KayttajaHelper.createEntity(em, DEFAULT_VASTUUHENKILO_ID)
            em.persist(vastuuhenkilo)
            return KoejaksonKoulutussopimus(
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanSyntymaaika = DEFAULT_SYNTYMAAIKA,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                opintooikeudenMyontamispaiva = DEFAULT_MYONTAMISPAIVA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                erikoistuvanPuhelinnumero = erikoistuvaLaakari.puhelinnumero,
                erikoistuvanSahkoposti = erikoistuvaLaakari.sahkoposti,
                lahetetty = false,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
                vastuuhenkilonNimi = vastuuhenkilo.nimi,
                vastuuhenkilonNimike = vastuuhenkilo.nimike,
                koejakso = erikoistuvaLaakari.koejakso
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKouluttaja(
            em: EntityManager,
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
            userId: String? = DEFAULT_KOULUTTAJA_ID
        ): KoulutussopimuksenKouluttaja {
            val kouluttaja = KayttajaHelper.createEntity(em, userId)
            em.persist(kouluttaja)
            return KoulutussopimuksenKouluttaja(
                kouluttaja = kouluttaja,
                nimi = kouluttaja.nimi,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKoulutuspaikka(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
        ): KoulutussopimuksenKoulutuspaikka {
            return KoulutussopimuksenKoulutuspaikka(
                nimi = DEFAULT_KOULUTUSPAIKKA,
                yliopisto = DEFAULT_YLIOPISTO,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }
    }
}
