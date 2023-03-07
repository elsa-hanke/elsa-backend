package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.SeurantajaksoMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import jakarta.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariSeurantakeskustelutResourceIT {
    @Autowired
    private lateinit var seurantajaksoRepository: SeurantajaksoRepository

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var suoritemerkintaRepository: SuoritemerkintaRepository

    @Autowired
    private lateinit var teoriakoulutusRepository: TeoriakoulutusRepository

    @Autowired
    private lateinit var koulutusjaksoRepository: KoulutusjaksoRepository

    @Autowired
    private lateinit var seurantajaksoMapper: SeurantajaksoMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restSeurantajaksoMockMvc: MockMvc

    private lateinit var seurantajakso: Seurantajakso

    private lateinit var user: User

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var kouluttaja: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun createSeurantajakso() {
        initTest()
        val databaseSizeBeforeCreate = seurantajaksoRepository.findAll().size

        val tapahtumaPvm = DEFAULT_ALKAMISPAIVA.plusDays(1)
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em, user, tapahtumaPvm)
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)
        val suoritemerkinta =
            SuoritemerkintaHelper.createEntity(em, user = user, suorituspaiva = tapahtumaPvm)
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)
        val teoriakoulutus = TeoriakoulutusHelper.createEntity(em, user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)
        seurantajakso.koulutusjaksot = mutableSetOf(koulutusjakso)

        val seurantajaksoDTO = seurantajaksoMapper.toDto(seurantajakso)
        seurantajaksoDTO.hyvaksytty = null
        restSeurantajaksoMockMvc.perform(
            post("$ENTITY_API_URL/seurantajakso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeCreate + 1)
        val testSeurantajakso = seurantajaksoList[seurantajaksoList.size - 1]

        assertThat(testSeurantajakso.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testSeurantajakso.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testSeurantajakso.opintooikeus).isEqualTo(erikoistuvaLaakari.getOpintooikeusKaytossa())
        assertThat(testSeurantajakso.kouluttaja).isEqualTo(kouluttaja)
        assertThat(testSeurantajakso.omaArviointi).isEqualTo(DEFAULT_OMA_ARVIOINTI)
        assertThat(testSeurantajakso.lisahuomioita).isEqualTo(DEFAULT_LISAHUOMIOITA)
        assertThat(testSeurantajakso.seuraavanJaksonTavoitteet).isEqualTo(DEFAULT_TAVOITTEET)

        val newArviointi = suoritusarviointiRepository.findById(suoritusarviointi.id!!)
        assertThat(newArviointi.get().lukittu).isTrue

        val newMerkinta = suoritemerkintaRepository.findById(suoritemerkinta.id!!)
        assertThat(newMerkinta.get().lukittu).isTrue

        val newKoulutus = koulutusjaksoRepository.findById(koulutusjakso.id!!)
        assertThat(newKoulutus.get().lukittu).isTrue
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createSeurantajaksoWithExistingId() {
        initTest()
        val databaseSizeBeforeCreate = seurantajaksoRepository.findAll().size

        val seurantajaksoDTO = seurantajaksoMapper.toDto(seurantajakso)
        seurantajaksoDTO.id = 1L
        restSeurantajaksoMockMvc.perform(
            post("$ENTITY_API_URL/seurantajakso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkOmaArviointiIsRequired() {
        initTest()
        val databaseSizeBeforeCreate = seurantajaksoRepository.findAll().size

        val seurantajaksoDTO = seurantajaksoMapper.toDto(seurantajakso)
        seurantajaksoDTO.omaArviointi = null
        restSeurantajaksoMockMvc.perform(
            post("$ENTITY_API_URL/seurantajakso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkAlkamispaivaIsRequired() {
        initTest()
        val databaseSizeBeforeCreate = seurantajaksoRepository.findAll().size

        val seurantajaksoDTO = seurantajaksoMapper.toDto(seurantajakso)
        seurantajaksoDTO.alkamispaiva = null
        restSeurantajaksoMockMvc.perform(
            post("$ENTITY_API_URL/seurantajakso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkPaattymispaivaIsRequired() {
        initTest()
        val databaseSizeBeforeCreate = seurantajaksoRepository.findAll().size

        val seurantajaksoDTO = seurantajaksoMapper.toDto(seurantajakso)
        seurantajaksoDTO.paattymispaiva = null
        restSeurantajaksoMockMvc.perform(
            post("$ENTITY_API_URL/seurantajakso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun getAllSeurantajaksot() {
        initTest()
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        restSeurantajaksoMockMvc.perform(get("$ENTITY_API_URL/seurantajaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].id").value(seurantajakso.id?.toInt()))
            .andExpect(jsonPath("$[0].alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$[0].paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$[0].omaArviointi").value(DEFAULT_OMA_ARVIOINTI))
            .andExpect(jsonPath("$[0].lisahuomioita").value(DEFAULT_LISAHUOMIOITA))
            .andExpect(jsonPath("$[0].seuraavanJaksonTavoitteet").value(DEFAULT_TAVOITTEET))
    }

    @Test
    @Transactional
    fun getAllSeurantajaksotShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val newOpintooikeus =
            OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        val seurantajaksoForAnotherOpintooikeus = createEntity(erikoistuvaLaakari, kouluttaja)
        em.persist(seurantajaksoForAnotherOpintooikeus)

        restSeurantajaksoMockMvc.perform(get("$ENTITY_API_URL/seurantajaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].id").value(seurantajaksoForAnotherOpintooikeus.id))
    }

    @Test
    @Transactional
    fun getSeurantajaksonTiedot() {
        initTest()
        var tapahtumaPvm = DEFAULT_ALKAMISPAIVA.plusDays(1)

        // In
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em, user, tapahtumaPvm)
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)
        val suoritemerkinta =
            SuoritemerkintaHelper.createEntity(em, user = user, suorituspaiva = tapahtumaPvm)
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)
        val teoriakoulutus = TeoriakoulutusHelper.createEntity(em, user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        // Out
        tapahtumaPvm = DEFAULT_PAATTYMISPAIVA.plusDays(1)
        val suoritusarviointiOut = SuoritusarviointiHelper.createEntity(em, user, tapahtumaPvm)
        suoritusarviointiRepository.saveAndFlush(suoritusarviointiOut)
        val suoritemerkintaOut =
            SuoritemerkintaHelper.createEntity(em, user = user, suorituspaiva = tapahtumaPvm)
        suoritemerkintaRepository.saveAndFlush(suoritemerkintaOut)
        val teoriakoulutusOut = TeoriakoulutusHelper.createEntity(em, user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutusOut)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        restSeurantajaksoMockMvc.perform(
            get("$ENTITY_API_URL/seurantajaksontiedot")
                .param("alkamispaiva", DEFAULT_ALKAMISPAIVA.toString())
                .param("paattymispaiva", DEFAULT_PAATTYMISPAIVA.toString())
                .param("koulutusjaksot", koulutusjakso.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.osaamistavoitteet").value(hasSize<Int>(0)))
            .andExpect(jsonPath("$.muutOsaamistavoitteet").value(hasItem(koulutusjakso.muutOsaamistavoitteet)))
            .andExpect(jsonPath("$.arvioinnit").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$.suoritemerkinnat").value(hasSize<Int>(1)))
            .andExpect(jsonPath("$.teoriakoulutukset").value(hasSize<Int>(1)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getSeurantajakso() {
        initTest()
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val id = seurantajakso.id
        assertNotNull(id)

        restSeurantajaksoMockMvc.perform(get(ENTITY_API_URL_ID, seurantajakso.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(seurantajakso.id?.toInt()))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.omaArviointi").value(DEFAULT_OMA_ARVIOINTI))
            .andExpect(jsonPath("$.lisahuomioita").value(DEFAULT_LISAHUOMIOITA))
            .andExpect(
                jsonPath("$.seuraavanJaksonTavoitteet").value(DEFAULT_TAVOITTEET)
            )
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingSeurantajakso() {
        initTest()
        restSeurantajaksoMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateSeurantajakso() {
        initTest()
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val databaseSizeBeforeUpdate = seurantajaksoRepository.findAll().size

        val updatedSeurantajakso = seurantajaksoRepository.findById(seurantajakso.id!!).get()
        em.detach(updatedSeurantajakso)
        updatedSeurantajakso.seurantakeskustelunYhteisetMerkinnat = UPDATED_YHTEISET_MERKINNAT
        updatedSeurantajakso.seuraavanKeskustelunAjankohta = UPDATED_SEURAAVA_AJANKOHTA
        updatedSeurantajakso.omaArviointi = UPDATED_OMA_ARVIOINTI
        updatedSeurantajakso.lisahuomioita = UPDATED_LISAHUOMIOITA
        updatedSeurantajakso.seuraavanJaksonTavoitteet = UPDATED_TAVOITTEET
        val seurantajaksoDTO = seurantajaksoMapper.toDto(updatedSeurantajakso)

        restSeurantajaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, seurantajakso.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeUpdate)
        val testSeurantajakso = seurantajaksoList[seurantajaksoList.size - 1]
        assertThat(testSeurantajakso.seurantakeskustelunYhteisetMerkinnat).isEqualTo(
            UPDATED_YHTEISET_MERKINNAT
        )
        assertThat(testSeurantajakso.seuraavanKeskustelunAjankohta).isEqualTo(
            UPDATED_SEURAAVA_AJANKOHTA
        )
        assertThat(testSeurantajakso.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testSeurantajakso.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testSeurantajakso.omaArviointi).isEqualTo(UPDATED_OMA_ARVIOINTI)
        assertThat(testSeurantajakso.lisahuomioita).isEqualTo(UPDATED_OMA_ARVIOINTI)
        assertThat(testSeurantajakso.seuraavanJaksonTavoitteet).isEqualTo(UPDATED_TAVOITTEET)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun updateNonExistingSeurantajakso() {
        initTest()
        val databaseSizeBeforeUpdate = seurantajaksoRepository.findAll().size
        seurantajakso.id = count.incrementAndGet()

        val seurantajaksoDTO = seurantajaksoMapper.toDto(seurantajakso)

        restSeurantajaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, seurantajakso.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(seurantajaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteSeurantajakso() {
        initTest()
        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjakso.lukittu = true
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)
        seurantajakso.koulutusjaksot = mutableSetOf(koulutusjakso)
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val tapahtumaPvm = DEFAULT_ALKAMISPAIVA.plusDays(1)
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em, user, tapahtumaPvm)
        suoritusarviointi.lukittu = true
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)
        val suoritemerkinta =
            SuoritemerkintaHelper.createEntity(em, user = user, suorituspaiva = tapahtumaPvm)
        suoritemerkinta.lukittu = true
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)
        val teoriakoulutus = TeoriakoulutusHelper.createEntity(em, user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val databaseSizeBeforeDelete = seurantajaksoRepository.findAll().size

        restSeurantajaksoMockMvc.perform(
            delete(ENTITY_API_URL_ID, seurantajakso.id).with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeDelete - 1)

        val newArviointi = suoritusarviointiRepository.findById(suoritusarviointi.id!!)
        assertThat(newArviointi.get().lukittu).isFalse

        val newMerkinta = suoritemerkintaRepository.findById(suoritemerkinta.id!!)
        assertThat(newMerkinta.get().lukittu).isFalse

        val newKoulutus = koulutusjaksoRepository.findById(koulutusjakso.id!!)
        assertThat(newKoulutus.get().lukittu).isFalse
    }

    @Test
    @Transactional
    fun deleteSeurantajaksoOverlapping() {
        initTest()
        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, user)
        koulutusjakso.lukittu = true
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        seurantajakso.koulutusjaksot = mutableSetOf(koulutusjakso)
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val overlappingSeurantajakso = createEntity(erikoistuvaLaakari, kouluttaja)
        overlappingSeurantajakso.koulutusjaksot = mutableSetOf(koulutusjakso)
        seurantajaksoRepository.saveAndFlush(overlappingSeurantajakso)

        val tapahtumaPvm = DEFAULT_ALKAMISPAIVA.plusDays(1)
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em, user, tapahtumaPvm)
        suoritusarviointi.lukittu = true
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)
        val suoritemerkinta =
            SuoritemerkintaHelper.createEntity(em, user = user, suorituspaiva = tapahtumaPvm)
        suoritemerkinta.lukittu = true
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)
        val teoriakoulutus = TeoriakoulutusHelper.createEntity(em, user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val databaseSizeBeforeDelete = seurantajaksoRepository.findAll().size

        restSeurantajaksoMockMvc.perform(
            delete(ENTITY_API_URL_ID, seurantajakso.id).with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        val seurantajaksoList = seurantajaksoRepository.findAll()
        assertThat(seurantajaksoList).hasSize(databaseSizeBeforeDelete - 1)

        val newArviointi = suoritusarviointiRepository.findById(suoritusarviointi.id!!)
        assertThat(newArviointi.get().lukittu).isTrue

        val newMerkinta = suoritemerkintaRepository.findById(suoritemerkinta.id!!)
        assertThat(newMerkinta.get().lukittu).isTrue

        val newKoulutus = koulutusjaksoRepository.findById(koulutusjakso.id!!)
        assertThat(newKoulutus.get().lukittu).isTrue
    }

    fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)

        kouluttaja = KayttajaHelper.createEntity(em)
        em.persist(kouluttaja)

        seurantajakso = createEntity(erikoistuvaLaakari, kouluttaja)
    }

    companion object {

        private const val DEFAULT_OMA_ARVIOINTI = "AAAAAAAAAA"
        private const val UPDATED_OMA_ARVIOINTI = "BBBBBBBBBB"

        private const val DEFAULT_LISAHUOMIOITA = "AAAAAAAAAA"
        private const val UPDATED_LISAHUOMIOITA = "BBBBBBBBBB"

        private const val DEFAULT_TAVOITTEET = "AAAAAAAAAA"
        private const val UPDATED_TAVOITTEET = "BBBBBBBBBB"

        private const val UPDATED_YHTEISET_MERKINNAT = "AAAAAAAAAA"
        private val UPDATED_SEURAAVA_AJANKOHTA = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val DEFAULT_PAATTYMISPAIVA: LocalDate = DEFAULT_ALKAMISPAIVA.plusDays(10)

        private const val ENTITY_API_URL: String = "/api/erikoistuva-laakari/seurantakeskustelut"
        private const val ENTITY_API_URL_ID: String = "$ENTITY_API_URL/seurantajakso/{id}"

        private val random: Random = SecureRandom()
        private val count: AtomicLong =
            AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))

        @JvmStatic
        fun createEntity(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            kouluttaja: Kayttaja
        ): Seurantajakso {
            return Seurantajakso(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                omaArviointi = DEFAULT_OMA_ARVIOINTI,
                lisahuomioita = DEFAULT_LISAHUOMIOITA,
                seuraavanJaksonTavoitteet = DEFAULT_TAVOITTEET,
                kouluttaja = kouluttaja
            )
        }

        @JvmStatic
        fun createUpdatedEntity(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            kouluttaja: Kayttaja
        ): Seurantajakso {
            return Seurantajakso(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                omaArviointi = UPDATED_OMA_ARVIOINTI,
                lisahuomioita = UPDATED_LISAHUOMIOITA,
                seuraavanJaksonTavoitteet = UPDATED_TAVOITTEET,
                kouluttaja = kouluttaja,
                seurantakeskustelunYhteisetMerkinnat = UPDATED_YHTEISET_MERKINNAT,
                seuraavanKeskustelunAjankohta = UPDATED_SEURAAVA_AJANKOHTA
            )
        }
    }
}
