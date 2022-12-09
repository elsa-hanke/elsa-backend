package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoUpdateDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
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
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VirkailijaTerveyskeskuskoulutusjaksoResourceIT {

    @Autowired
    private lateinit var terveyskeskuskoulutusjaksonHyvaksyntaRepository: TerveyskeskuskoulutusjaksonHyvaksyntaRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var terveyskeskuskoulutusjaksonHyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta

    private lateinit var user: User

    private lateinit var virkailija: Kayttaja

    private lateinit var vastuuhenkilo: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksot() {
        initTest()
        restKoejaksoMockMvc.perform(get("/api/virkailija/terveyskeskuskoulutusjaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(terveyskeskuskoulutusjaksonHyvaksynta.id))
            .andExpect(jsonPath("$.content[0].tila").value(TerveyskeskuskoulutusjaksoTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA.name))
            .andExpect(
                jsonPath("$.content[0].erikoistuvanNimi").value(
                    terveyskeskuskoulutusjaksonHyvaksynta.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()
                )
            )
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksotEriYliopisto() {
        val yliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(yliopisto)
        initTest(yliopisto)

        restKoejaksoMockMvc.perform(get("/api/virkailija/terveyskeskuskoulutusjaksot"))
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
                "/api/virkailija/terveyskeskuskoulutusjaksot",
                NimiErikoisalaAndAvoinCriteria(avoin = true)
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(terveyskeskuskoulutusjaksonHyvaksynta.id))
            .andExpect(jsonPath("$.content[0].tila").value(TerveyskeskuskoulutusjaksoTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA.name))
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

        terveyskeskuskoulutusjaksonHyvaksynta.virkailijaHyvaksynyt = true
        terveyskeskuskoulutusjaksonHyvaksyntaRepository.saveAndFlush(
            terveyskeskuskoulutusjaksonHyvaksynta
        )

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/terveyskeskuskoulutusjaksot",
                NimiErikoisalaAndAvoinCriteria(avoin = false)
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
    fun getTerveyskeskuskoulutusjakso() {
        initTest()

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/terveyskeskuskoulutusjakso/{id}", id
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
            .andExpect(jsonPath("$.tila").value(TerveyskeskuskoulutusjaksoTila.ODOTTAA_VIRKAILIJAN_TARKISTUSTA.toString()))
            .andExpect(jsonPath("$.korjausehdotus").doesNotExist())
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksoEriYliopisto() {
        val yliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(yliopisto)
        initTest(yliopisto)

        val id = terveyskeskuskoulutusjaksonHyvaksynta.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/terveyskeskuskoulutusjakso/{id}", id
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
                korjausehdotus = null,
                lisatiedotVirkailijalta = "test"
            )

        restKoejaksoMockMvc.perform(
            put("/api/virkailija/terveyskeskuskoulutusjakson-hyvaksynta/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(terveyskeskuskoulutusjaksoUpdateDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val hyvaksyntaList = terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll()
        assertThat(hyvaksyntaList).hasSize(databaseSizeBeforeUpdate)
        val testHyvaksynta = hyvaksyntaList[hyvaksyntaList.size - 1]
        assertThat(testHyvaksynta.virkailijaHyvaksynyt).isTrue
        assertThat(testHyvaksynta.vastuuhenkiloHyvaksynyt).isFalse
        assertThat(testHyvaksynta.lisatiedotVirkailijalta).isEqualTo("test")
        assertThat(testHyvaksynta.korjausehdotus).isNull()
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
            TerveyskeskuskoulutusjaksoUpdateDTO(
                korjausehdotus = "test",
                lisatiedotVirkailijalta = null
            )

        restKoejaksoMockMvc.perform(
            put("/api/virkailija/terveyskeskuskoulutusjakson-hyvaksynta/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(terveyskeskuskoulutusjaksoUpdateDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val hyvaksyntaList = terveyskeskuskoulutusjaksonHyvaksyntaRepository.findAll()
        assertThat(hyvaksyntaList).hasSize(databaseSizeBeforeUpdate)
        val testHyvaksynta = hyvaksyntaList[hyvaksyntaList.size - 1]
        assertThat(testHyvaksynta.virkailijaHyvaksynyt).isFalse
        assertThat(testHyvaksynta.vastuuhenkiloHyvaksynyt).isFalse
        assertThat(testHyvaksynta.korjausehdotus).isEqualTo("test")
    }

    fun initTest(virkailijaYliopisto: Yliopisto? = null) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(OPINTOHALLINNON_VIRKAILIJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        val yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO)
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

        val vastuuhenkiloUser =
            KayttajaResourceWithMockUserIT.createEntity(
                authority = Authority(
                    VASTUUHENKILO
                )
            )
        em.persist(vastuuhenkiloUser)
        vastuuhenkilo = KayttajaHelper.createEntity(em, user = vastuuhenkiloUser)
        em.persist(vastuuhenkilo)

        em.persist(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkilo,
                yliopisto = yliopisto,
                erikoisala = Erikoisala(50),
                vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))
            )
        )

        virkailija = KayttajaHelper.createEntity(em, user)

        if (virkailijaYliopisto != null) {
            virkailija.yliopistot.add(virkailijaYliopisto)
        } else {
            virkailija.yliopistot.add(yliopisto)
        }
        em.persist(virkailija)

        terveyskeskuskoulutusjaksonHyvaksynta =
            createTerveyskeskuskoulutusjaksonHyvaksynta(erikoistuvaLaakari)
        em.persist(terveyskeskuskoulutusjaksonHyvaksynta)
    }

    companion object {

        private val DEFAULT_LAILLISTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(15L)

        @JvmStatic
        fun createTerveyskeskuskoulutusjaksonHyvaksynta(
            erikoistuvaLaakari: ErikoistuvaLaakari
        ): TerveyskeskuskoulutusjaksonHyvaksynta {
            return TerveyskeskuskoulutusjaksonHyvaksynta(
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                muokkauspaiva = KoejaksonVaiheetHelper.DEFAULT_MUOKKAUSPAIVA
            )
        }
    }
}
