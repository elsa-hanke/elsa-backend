package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.mapper.SeurantajaksoMapper
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KouluttajaSeurantakeskustelutResourceIT {
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
    fun getAllSeurantajaksot() {
        initTest()
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val erikoistuvaLaakari2 = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari2)

        val seurantajakso2 = createEntity(erikoistuvaLaakari2, kouluttaja)
        seurantajakso2.kouluttajanArvio = DEFAULT_KOULUTTAJAN_ARVIO
        seurantajakso2.edistyminenTavoitteidenMukaista = true
        seurantajakso2.tallennettu = LocalDate.ofEpochDay(1L)
        seurantajaksoRepository.saveAndFlush(seurantajakso2)

        restSeurantajaksoMockMvc.perform(get("$ENTITY_API_URL/seurantajaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].id").value(seurantajakso.id?.toInt()))
            .andExpect(jsonPath("$[0].alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$[0].paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$[0].omaArviointi").value(DEFAULT_OMA_ARVIOINTI))
            .andExpect(jsonPath("$[0].lisahuomioita").value(DEFAULT_LISAHUOMIOITA))
            .andExpect(jsonPath("$[0].seuraavanJaksonTavoitteet").value(DEFAULT_TAVOITTEET))
            .andExpect(jsonPath("$[1].id").value(seurantajakso2.id?.toInt()))
            .andExpect(jsonPath("$[1].alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$[1].paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$[1].omaArviointi").value(DEFAULT_OMA_ARVIOINTI))
            .andExpect(jsonPath("$[1].lisahuomioita").value(DEFAULT_LISAHUOMIOITA))
            .andExpect(jsonPath("$[1].seuraavanJaksonTavoitteet").value(DEFAULT_TAVOITTEET))
            .andExpect(jsonPath("$[1].edistyminenTavoitteidenMukaista").value(true))
            .andExpect(jsonPath("$[1].kouluttajanArvio").value(DEFAULT_KOULUTTAJAN_ARVIO))
    }

    @Test
    @Transactional
    fun getSeurantajaksonTiedot() {
        initTest()
        var tapahtumaPvm = DEFAULT_ALKAMISPAIVA.plusDays(1)

        // In
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(
            em,
            user,
            tapahtumaPvm
        )
        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)
        val suoritemerkinta =
            SuoritemerkintaHelper.createEntity(
                em,
                user = erikoistuvaLaakari.kayttaja?.user,
                suorituspaiva = tapahtumaPvm
            )
        suoritemerkintaRepository.saveAndFlush(suoritemerkinta)
        val teoriakoulutus =
            TeoriakoulutusHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        // Out
        tapahtumaPvm = DEFAULT_PAATTYMISPAIVA.plusDays(1)
        val suoritusarviointiOut = SuoritusarviointiHelper.createEntity(
            em,
            erikoistuvaLaakari.kayttaja?.user,
            tapahtumaPvm
        )
        suoritusarviointiRepository.saveAndFlush(suoritusarviointiOut)
        val suoritemerkintaOut =
            SuoritemerkintaHelper.createEntity(
                em,
                user = erikoistuvaLaakari.kayttaja?.user,
                suorituspaiva = tapahtumaPvm
            )
        suoritemerkintaRepository.saveAndFlush(suoritemerkintaOut)
        val teoriakoulutusOut =
            TeoriakoulutusHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user, tapahtumaPvm)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutusOut)

        val koulutusjakso = KoulutusjaksoHelper.createEntity(em, erikoistuvaLaakari.kayttaja?.user)
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        seurantajakso.koulutusjaksot?.add(koulutusjakso)
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        restSeurantajaksoMockMvc.perform(
            get("$ENTITY_API_URL/seurantajaksontiedot")
                .param("id", seurantajakso.id.toString())
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
        updatedSeurantajakso.kouluttajanArvio = DEFAULT_KOULUTTAJAN_ARVIO
        updatedSeurantajakso.edistyminenTavoitteidenMukaista = false
        updatedSeurantajakso.huolenaiheet = DEFAULT_HUOLENAIHEET
        updatedSeurantajakso.erikoisalanTyoskentelyvalmiudet = DEFAULT_TYOSKENTELYVALMIUDET
        updatedSeurantajakso.jatkotoimetJaRaportointi = DEFAULT_JATKOTOIMET
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
        assertThat(testSeurantajakso.edistyminenTavoitteidenMukaista).isEqualTo(false)
        assertThat(testSeurantajakso.huolenaiheet).isEqualTo(DEFAULT_HUOLENAIHEET)
        assertThat(testSeurantajakso.kouluttajanArvio).isEqualTo(DEFAULT_KOULUTTAJAN_ARVIO)
        assertThat(testSeurantajakso.erikoisalanTyoskentelyvalmiudet).isEqualTo(
            DEFAULT_TYOSKENTELYVALMIUDET
        )
        assertThat(testSeurantajakso.jatkotoimetJaRaportointi).isEqualTo(DEFAULT_JATKOTOIMET)
        assertThat(testSeurantajakso.hyvaksytty).isNull()
    }

    @Test
    @Transactional
    fun updateSeurantajaksoWithMerkinnat() {
        initTest()
        seurantajakso = createUpdatedEntity(erikoistuvaLaakari, kouluttaja)
        seurantajakso.kouluttajanArvio = DEFAULT_KOULUTTAJAN_ARVIO
        seurantajakso.edistyminenTavoitteidenMukaista = false
        seurantajakso.huolenaiheet = DEFAULT_HUOLENAIHEET
        seurantajakso.erikoisalanTyoskentelyvalmiudet = DEFAULT_TYOSKENTELYVALMIUDET
        seurantajakso.jatkotoimetJaRaportointi = DEFAULT_JATKOTOIMET
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val databaseSizeBeforeUpdate = seurantajaksoRepository.findAll().size

        val updatedSeurantajakso = seurantajaksoRepository.findById(seurantajakso.id!!).get()
        em.detach(updatedSeurantajakso)
        updatedSeurantajakso.kouluttajanArvio = UPDATED_KOULUTTAJAN_ARVIO
        updatedSeurantajakso.edistyminenTavoitteidenMukaista = false
        updatedSeurantajakso.huolenaiheet = UPDATED_HUOLENAIHEET
        updatedSeurantajakso.erikoisalanTyoskentelyvalmiudet = UPDATED_TYOSKENTELYVALMIUDET
        updatedSeurantajakso.jatkotoimetJaRaportointi = UPDATED_JATKOTOIMET
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
        assertThat(testSeurantajakso.edistyminenTavoitteidenMukaista).isEqualTo(false)
        assertThat(testSeurantajakso.huolenaiheet).isEqualTo(UPDATED_HUOLENAIHEET)
        assertThat(testSeurantajakso.kouluttajanArvio).isEqualTo(UPDATED_KOULUTTAJAN_ARVIO)
        assertThat(testSeurantajakso.erikoisalanTyoskentelyvalmiudet).isEqualTo(
            UPDATED_TYOSKENTELYVALMIUDET
        )
        assertThat(testSeurantajakso.jatkotoimetJaRaportointi).isEqualTo(UPDATED_JATKOTOIMET)
        assertThat(testSeurantajakso.hyvaksytty).isEqualTo(true)
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
    fun returnSeurantajakso() {
        initTest()
        seurantajaksoRepository.saveAndFlush(seurantajakso)

        val databaseSizeBeforeUpdate = seurantajaksoRepository.findAll().size

        val updatedSeurantajakso = seurantajaksoRepository.findById(seurantajakso.id!!).get()
        em.detach(updatedSeurantajakso)
        updatedSeurantajakso.korjausehdotus = DEFAULT_KORJAUSEHDOTUS
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
        assertThat(testSeurantajakso.korjausehdotus).isEqualTo(DEFAULT_KORJAUSEHDOTUS)
        assertThat(testSeurantajakso.hyvaksytty).isNull()
    }

    fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(KOULUTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)

        kouluttaja = KayttajaHelper.createEntity(em, user)
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

        private const val DEFAULT_KOULUTTAJAN_ARVIO = "AAAAAAAAAA"
        private const val UPDATED_KOULUTTAJAN_ARVIO = "BBBBBBBBBB"

        private const val DEFAULT_HUOLENAIHEET = "AAAAAAAAAA"
        private const val UPDATED_HUOLENAIHEET = "BBBBBBBBBB"

        private const val DEFAULT_TYOSKENTELYVALMIUDET = "AAAAAAAAAA"
        private const val UPDATED_TYOSKENTELYVALMIUDET = "BBBBBBBBBB"

        private const val DEFAULT_JATKOTOIMET = "AAAAAAAAAA"
        private const val UPDATED_JATKOTOIMET = "BBBBBBBBBB"

        private const val DEFAULT_KORJAUSEHDOTUS = "AAAAAAAAAA"

        private const val ENTITY_API_URL: String = "/api/kouluttaja/seurantakeskustelut"
        private const val ENTITY_API_URL_ID: String = "$ENTITY_API_URL/seurantajakso/{id}"

        private val random: Random = Random()
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
