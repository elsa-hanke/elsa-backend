package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import org.assertj.core.api.Assertions.assertThat
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
import jakarta.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VirkailijaKoejaksoResourceIT {

    @Autowired
    private lateinit var koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejaksonVastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio

    private lateinit var user: User

    private lateinit var virkailija: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKoejaksotByVirkailija() {
        initTest()

        koejaksonVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = false
        vastuuhenkilonArvioRepository.saveAndFlush(koejaksonVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(get("/api/virkailija/koejaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(koejaksonVastuuhenkilonArvio.id))
            .andExpect(jsonPath("$.content[0].tila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$.content[0].tyyppi").value(KoejaksoTyyppi.VASTUUHENKILON_ARVIO.name))
            .andExpect(jsonPath("$.content[0].erikoistuvanNimi").value(koejaksonVastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
    }

    @Test
    @Transactional
    fun getKoejaksotByVirkailijaAvoin() {
        initTest()

        koejaksonVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = false
        vastuuhenkilonArvioRepository.saveAndFlush(koejaksonVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/koejaksot",
                NimiErikoisalaAndAvoinCriteria(avoin = true)
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(koejaksonVastuuhenkilonArvio.id))
            .andExpect(jsonPath("$.content[0].tila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$.content[0].tyyppi").value(KoejaksoTyyppi.VASTUUHENKILON_ARVIO.name))
            .andExpect(jsonPath("$.content[0].erikoistuvanNimi").value(koejaksonVastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
    }

    @Test
    @Transactional
    fun getKoejaksotByVirkailijaMuut() {
        initTest()

        koejaksonVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = true
        vastuuhenkilonArvioRepository.saveAndFlush(koejaksonVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/koejaksot",
                NimiErikoisalaAndAvoinCriteria(avoin = false)
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(koejaksonVastuuhenkilonArvio.id))
            .andExpect(jsonPath("$.content[0].tila").value(KoejaksoTila.ODOTTAA_VASTUUHENKILON_ALLEKIRJOITUSTA.name))
            .andExpect(jsonPath("$.content[0].tyyppi").value(KoejaksoTyyppi.VASTUUHENKILON_ARVIO.name))
            .andExpect(jsonPath("$.content[0].erikoistuvanNimi").value(koejaksonVastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
    }

    @Test
    @Transactional
    fun getVastuuhenkilonArvio() {
        initTest()

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/koejakso/vastuuhenkilonarvio/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonVastuuhenkilonArvio.id))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonVastuuhenkilonArvio.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonVastuuhenkilonArvio.opintooikeus?.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonVastuuhenkilonArvio.opintooikeus?.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(koejaksonVastuuhenkilonArvio.vastuuhenkilo?.id))
    }

    @Test
    @Transactional
    fun getVastuuhenkilonArvioEriYliopisto() {
        val yliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(yliopisto)
        initTest(yliopisto)

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/virkailija/koejakso/vastuuhenkilonarvio/{id}", id
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun ackVastuuhenkilonArvio() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)
        val updatedVastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findById(id).get()
        em.detach(updatedVastuuhenkilonArvio)

        updatedVastuuhenkilonArvio.korjausehdotus = null

        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(updatedVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            put("/api/virkailija/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(vastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeUpdate)
        val testVastuuhenkilonArvio = vastuuhenkilonArvioList[vastuuhenkilonArvioList.size - 1]
        assertThat(testVastuuhenkilonArvio.virkailijaHyvaksynyt).isEqualTo(true)
        assertThat(testVastuuhenkilonArvio.vastuuhenkiloHyvaksynyt).isEqualTo(false)
        assertThat(testVastuuhenkilonArvio.virkailijanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineVastuuhenkilonArvio() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)
        val updatedVastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findById(id).get()
        em.detach(updatedVastuuhenkilonArvio)

        updatedVastuuhenkilonArvio.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(updatedVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            put("/api/virkailija/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(vastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeUpdate)
        val testVastuuhenkilonArvio = vastuuhenkilonArvioList[vastuuhenkilonArvioList.size - 1]
        assertThat(testVastuuhenkilonArvio.virkailijaHyvaksynyt).isEqualTo(false)
        assertThat(testVastuuhenkilonArvio.virkailijanKuittausaika).isNull()
        assertThat(testVastuuhenkilonArvio.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
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
            ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)

        val vastuuhenkilo = KayttajaHelper.createEntity(em)
        em.persist(vastuuhenkilo)

        virkailija = KayttajaHelper.createEntity(em, user)

        if (virkailijaYliopisto != null) {
            virkailija.yliopistot.add(virkailijaYliopisto)
        } else {
            virkailija.yliopistot.add(yliopisto)
        }
        em.persist(virkailija)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
        em.persist(koejaksonVastuuhenkilonArvio)
    }

    companion object {

        @JvmStatic
        fun createVastuuhenkilonArvio(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonVastuuhenkilonArvio {
            return KoejaksonVastuuhenkilonArvio(
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                muokkauspaiva = KoejaksonVaiheetHelper.DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
            )
        }
    }
}
