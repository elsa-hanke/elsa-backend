package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoUpdateDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
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
import jakarta.persistence.EntityManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VastuuhenkiloTerveyskeskuskoulutusjaksoResourceIT {

    @Autowired
    private lateinit var terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var terveyskeskuskoulutusjaksonHyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta

    private lateinit var user: User

    private lateinit var virkailija: Kayttaja

    private lateinit var vastuuhenkilo: Kayttaja

    private lateinit var yliopisto: Yliopisto

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksot() {
        initTest()
        restKoejaksoMockMvc.perform(get("/api/vastuuhenkilo/terveyskeskuskoulutusjaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(terveyskeskuskoulutusjaksonHyvaksynta.id))
            .andExpect(jsonPath("$.content[0].tila").value(TerveyskeskuskoulutusjaksoTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.name))
            .andExpect(
                jsonPath("$.content[0].erikoistuvanNimi").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()
                )
            )
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksotEriYliopisto() {
        initTest(YliopistoEnum.HELSINGIN_YLIOPISTO)

        restKoejaksoMockMvc.perform(get("/api/vastuuhenkilo/terveyskeskuskoulutusjaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(0)))
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksotEriTehtavatyyppi() {
        initTest(tehtavatyyppi = VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN)

        restKoejaksoMockMvc.perform(get("/api/vastuuhenkilo/terveyskeskuskoulutusjaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(0)))
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksotAvoin() {
        initTest()

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/terveyskeskuskoulutusjaksot",
                NimiErikoisalaAndAvoinCriteria(avoin = true)
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(terveyskeskuskoulutusjaksonHyvaksynta.id))
            .andExpect(jsonPath("$.content[0].tila").value(TerveyskeskuskoulutusjaksoTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.name))
            .andExpect(
                jsonPath("$.content[0].erikoistuvanNimi").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()
                )
            )
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksotMuut() {
        initTest()

        terveyskeskuskoulutusjaksonHyvaksynta.vastuuhenkiloHyvaksynyt = true
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.saveAndFlush(
            terveyskeskuskoulutusjaksonHyvaksynta
        )

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/terveyskeskuskoulutusjaksot",
                NimiErikoisalaAndAvoinCriteria(avoin = false)
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(terveyskeskuskoulutusjaksonHyvaksynta.id))
            .andExpect(jsonPath("$.content[0].tila").value(TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY.name))
            .andExpect(
                jsonPath("$.content[0].erikoistuvanNimi").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()
                )
            )
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjakso() {
        initTest()

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/terveyskeskuskoulutusjakso/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(terveyskeskuskoulutusjaksonHyvaksynta.id))
            .andExpect(
                jsonPath("$.erikoistuvanErikoisala").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoisala?.nimi
                )
            )
            .andExpect(jsonPath("$.erikoistuvanNimi").value(terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(
                jsonPath("$.erikoistuvanOpiskelijatunnus").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.opiskelijatunnus
                )
            )
            .andExpect(
                jsonPath("$.erikoistuvanSyntymaaika").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.syntymaaika.toString()
                )
            )
            .andExpect(
                jsonPath("$.erikoistuvanYliopisto").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.yliopisto?.nimi.toString()
                )
            )
            .andExpect(jsonPath("$.laillistamispaiva").value(terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.laillistamispaiva.toString()))
            .andExpect(jsonPath("$.asetus").value(terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.asetus?.nimi))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimi").value(vastuuhenkilo.getNimi()))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimike").value(vastuuhenkilo.nimike))
            .andExpect(jsonPath("$.tila").value(TerveyskeskuskoulutusjaksoTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.toString()))
            .andExpect(jsonPath("$.korjausehdotus").doesNotExist())
            .andExpect(jsonPath("$.virkailijanNimi").value(virkailija.getNimi()))
            .andExpect(jsonPath("$.virkailijanNimike").value(virkailija.nimike))
            .andExpect(jsonPath("$.virkailijanKuittausaika").value(DEFAULT_VIRKAILIJAN_KUITTAUSAIKA.toString()))
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksoEriYliopisto() {
        initTest(YliopistoEnum.HELSINGIN_YLIOPISTO)

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/terveyskeskuskoulutusjakso/{id}", id
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksoEriTehtavatyyppi() {
        initTest(tehtavatyyppi = VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN)

        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                VASTUUHENKILO
            )
        )
        userRepository.saveAndFlush(vastuuhenkiloUser)

        val yleislaaketieteenVastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        yleislaaketieteenVastuuhenkilo.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = yleislaaketieteenVastuuhenkilo,
                yliopisto = yliopisto,
                erikoisala = Erikoisala(50),
                vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))
            )
        )
        kayttajaRepository.saveAndFlush(yleislaaketieteenVastuuhenkilo)

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/terveyskeskuskoulutusjakso/{id}", id
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun ackTerveyskeskuskoulutusjakso() {
        initTest()

        val databaseSizeBeforeUpdate =
            terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll().size

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        val terveyskeskuskoulutusjaksoUpdateDTO =
            TerveyskeskuskoulutusjaksoUpdateDTO(
                korjausehdotus = null
            )

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/terveyskeskuskoulutusjakson-hyvaksynta/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(terveyskeskuskoulutusjaksoUpdateDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val hyvaksyntaList = terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll()
        assertThat(hyvaksyntaList).hasSize(databaseSizeBeforeUpdate)
        val testHyvaksynta = hyvaksyntaList[hyvaksyntaList.size - 1]
        assertThat(testHyvaksynta.virkailijaHyvaksynyt).isTrue
        assertEquals(DEFAULT_VIRKAILIJAN_KUITTAUSAIKA, testHyvaksynta.virkailijanKuittausaika)
        assertThat(testHyvaksynta.vastuuhenkiloHyvaksynyt).isTrue
        assertThat(testHyvaksynta.vastuuhenkilonKuittausaika).isNotNull
        assertThat(testHyvaksynta.virkailijanKorjausehdotus).isNull()
        assertThat(testHyvaksynta.vastuuhenkilonKorjausehdotus).isNull()
    }

    @Test
    @Transactional
    fun declineTerveyskeskuskoulutusjakso() {
        initTest()

        val databaseSizeBeforeUpdate =
            terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll().size

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        val terveyskeskuskoulutusjaksoUpdateDTO =
            TerveyskeskuskoulutusjaksoUpdateDTO(korjausehdotus = "test")

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/terveyskeskuskoulutusjakson-hyvaksynta/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(terveyskeskuskoulutusjaksoUpdateDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val hyvaksyntaList = terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll()
        assertThat(hyvaksyntaList).hasSize(databaseSizeBeforeUpdate)
        val testHyvaksynta = hyvaksyntaList[hyvaksyntaList.size - 1]
        assertThat(testHyvaksynta.virkailijaHyvaksynyt).isFalse
        assertThat(testHyvaksynta.vastuuhenkiloHyvaksynyt).isFalse
        assertThat(testHyvaksynta.virkailijanKorjausehdotus).isNull()
        assertThat(testHyvaksynta.vastuuhenkilonKorjausehdotus).isEqualTo("test")
    }

    fun initTest(
        vastuuhenkilonYliopistoNimi: YliopistoEnum? = YliopistoEnum.TAMPEREEN_YLIOPISTO,
        tehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum? = VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN
    ) {
        user =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(name = VASTUUHENKILO))
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO)
        em.persist(yliopisto)

        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                yliopisto = yliopisto,
                laillistamispaiva = DEFAULT_LAILLISTAMISPAIVA
            )
        em.persist(erikoistuvaLaakari)

        val alkamispaiva = LocalDate.ofEpochDay(0L)
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(
            em,
            alkamispaiva = alkamispaiva,
            paattymispaiva = alkamispaiva.plusYears(2),
            user = erikoistuvaLaakari.kayttaja?.user,
            kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        )
        em.persist(tyoskentelyjakso)

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)

        var vastuuhenkilonYliopisto = yliopisto

        if (vastuuhenkilonYliopistoNimi != YliopistoEnum.TAMPEREEN_YLIOPISTO) {
            vastuuhenkilonYliopisto = Yliopisto(nimi = vastuuhenkilonYliopistoNimi)
            em.persist(vastuuhenkilonYliopisto)
        }

        vastuuhenkilo = KayttajaHelper.createEntity(em, user = user)
        vastuuhenkilo.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkilo,
                yliopisto = vastuuhenkilonYliopisto,
                erikoisala = Erikoisala(50),
                vastuuhenkilonTehtavat = mutableSetOf(tehtavatyypit.first { it.nimi == tehtavatyyppi })
            )
        )
        em.persist(vastuuhenkilo)

        virkailija = KayttajaHelper.createEntity(em)
        em.persist(virkailija)

        terveyskeskuskoulutusjaksonHyvaksynta =
            createTerveyskeskuskoulutusjaksonHyvaksynta(erikoistuvaLaakari, virkailija)
        em.persist(terveyskeskuskoulutusjaksonHyvaksynta)
    }

    companion object {

        private val DEFAULT_LAILLISTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(15L)
        private val DEFAULT_VIRKAILIJAN_KUITTAUSAIKA: LocalDate = LocalDate.ofEpochDay(20L)

        @JvmStatic
        fun createTerveyskeskuskoulutusjaksonHyvaksynta(
            erikoistuvaLaakari: ErikoistuvaLaakari, virkailija: Kayttaja
        ): TerveyskeskuskoulutusjaksonHyvaksynta {
            return TerveyskeskuskoulutusjaksonHyvaksynta(
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                muokkauspaiva = KoejaksonVaiheetHelper.DEFAULT_MUOKKAUSPAIVA,
                virkailija = virkailija,
                virkailijaHyvaksynyt = true,
                virkailijanKuittausaika = DEFAULT_VIRKAILIJAN_KUITTAUSAIKA
            )
        }
    }
}
