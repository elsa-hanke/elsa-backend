package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.VerificationToken
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.dto.KayttajaYliopistoErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.DEFAULT_YLIOPISTO
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajahallintaResourceHelper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

private const val TEKNINEN_PAAKAYTTAJA_ROLE_PATH = "tekninen-paakayttaja"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class PaakayttajaKayttajahallintaResourceIT: ResourceIntegrationTestBase() {

    @Autowired private lateinit var erikoisalaRepository: ErikoisalaRepository
    @Autowired private lateinit var kayttajaRepository: KayttajaRepository
    @Autowired private lateinit var erikoisalaMapper: ErikoisalaMapper

    private lateinit var yliopisto: Yliopisto

    @Test
    fun getKouluttajaForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        val erikoisala = erikoisalaRepository.findById(1).get()
        val id = KayttajahallintaResourceHelper.persistKouluttaja(em, yliopisto, erikoisala)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun getErikoistuvaLaakariForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        val id = KayttajahallintaResourceHelper.persistErikoistuvaLaakari(em, yliopisto)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/erikoistuvat-laakarit/$id")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.kayttaja.id").value(id))
            .andExpect(jsonPath("$.erikoistuvaLaakari.id").exists())
    }

    @Test
    fun getVastuuhenkiloForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        val erikoisala = erikoisalaRepository.findById(1).get()
        val id = KayttajahallintaResourceHelper.persistVastuuhenkilo(em, yliopisto, erikoisala)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun getVirkailijaForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        val id = KayttajahallintaResourceHelper.persistVirkailija(em, yliopisto)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun getPaakayttajaForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        val id = KayttajahallintaResourceHelper.persistPaakayttaja(em)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun patchPaakayttajaOnlyEmailUpdated() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = KayttajahallintaResourceHelper.createPaakayttaja(em).apply { user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN }
        em.persist(paakayttaja)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(sahkoposti = paakayttaja.user?.email + "x", eppn = paakayttaja.user?.eppn + "x")

        testMockMvc.perform(patch("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/virkailijat/${paakayttaja.id}").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedPaakayttaja = kayttajaRepository.findById(paakayttaja.id!!).get()
        assertThat(updatedPaakayttaja.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedPaakayttaja.user?.eppn).isEqualTo(paakayttaja.user?.eppn)
    }

    @Test
    fun patchPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = KayttajahallintaResourceHelper.createVirkailija(em, yliopisto).apply { user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN }
        paakayttaja.tila = KayttajatilinTila.KUTSUTTU
        em.persist(paakayttaja)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(sahkoposti = paakayttaja.user?.email + "x", eppn = paakayttaja.user?.eppn + "x")

        testMockMvc.perform(patch("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/virkailijat/${paakayttaja.id}").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedPaakayttaja = kayttajaRepository.findById(paakayttaja.id!!).get()
        assertThat(updatedPaakayttaja.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedPaakayttaja.user?.eppn).isEqualTo(kayttajahallintaKayttajaDTO.eppn)
    }

    @Test
    fun postPaakayttajaWithEmailAddressInUse() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        val paakayttaja = createPersistedPaakayttaja()
        paakayttaja.user?.email = KayttajaHelper.DEFAULT_EMAIL
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { sahkoposti = KayttajaHelper.DEFAULT_EMAIL }

        testMockMvc.perform(post("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/paakayttajat").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun postPaakayttajaWithEppnInUse() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = createPersistedPaakayttaja()
        paakayttaja.user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN
        paakayttaja.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x" }

        testMockMvc.perform(post("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/paakayttajat").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
            .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun postPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        testMockMvc.perform(post("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/paakayttajat").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(KayttajahallintaResourceHelper.getDefaultKayttajaDTO())).with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isCreated)
    }

    @Test
    fun getPaakayttajatForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)
        KayttajahallintaResourceHelper.createPaakayttaja(em)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/paakayttajat")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(2)))
    }

    @Test
    fun postVastuuhenkiloAsTekninenPaakayttajaWithDifferentYliopistoInYliopistotAndErikoisalat() {
        initTest(TEKNINEN_PAAKAYTTAJA)

        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisalaRepository.findById(1).get())

        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = YliopistoDTO(id = 1000)
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = YliopistoDTO(id = 1001), erikoisala = erikoisalaDTO))
        }

        testMockMvc.perform(post("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/vastuuhenkilot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun resendErikoistuvaLaakariInvitationByPaakayttajaDifferentYliopisto() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        persistAndFlush(erikoistuvaLaakari)
        em.persist(VerificationToken(user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)))

        testMockMvc.perform(put("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/erikoistuvat-laakarit/${erikoistuvaLaakari.id}/kutsu")
            .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isNoContent)
    }

    fun initTest(role: String, addDefaultYliopistoForRole: Boolean = false) {
        yliopisto = persistYliopisto(DEFAULT_YLIOPISTO)
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(role))
        persistAndFlush(user)
        val authentication = Saml2Authentication(DefaultSaml2AuthenticatedPrincipal(user.id, mapOf<String, List<Any>>()), "test", listOf(SimpleGrantedAuthority(role)))

        val kayttaja = KayttajaHelper.createEntity(em, user)
        if (role == OPINTOHALLINNON_VIRKAILIJA) {
            if (addDefaultYliopistoForRole) kayttaja.yliopistot.add(yliopisto) else kayttaja.yliopistot.add(persistYliopisto(YliopistoEnum.ITA_SUOMEN_YLIOPISTO))
        }
        em.persist(kayttaja)

        TestSecurityContextHolder.getContext().authentication = authentication
    }

}
