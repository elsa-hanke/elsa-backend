package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum.*
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole.*
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.*
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.*
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val ENDPOINT_BASE_URL = "/api/vastuuhenkilo"
private const val VALMISTUMISPYYNNOT_ENDPOINT = "/valmistumispyynnot?page=0&size=20&sort=muokkauspaiva,desc"
private const val VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT = "/valmistumispyynnon-arviointi"
private const val VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT = "/valmistumispyynto-arviointien-tila"
private const val VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT = "/valmistumispyynnon-hyvaksynta"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class VastuuhenkiloValmistumispyyntoResourceIT : ResourceIntegrationTestBase() {

    @Autowired private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository
    private lateinit var opintooikeus: Opintooikeus
    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari
    private lateinit var anotherVastuuhenkilo: Kayttaja

    private  val palautusaikaMinusDay1 = LocalDate.now().minusDays(1)

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaOsaamisenArviointiaAvoin() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ODOTTAA_VASTUUHENKILON_TARKASTUSTA.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaHyvaksyntaaAvoin() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_HYVAKSYJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaHyvaksyjaTilaOdottaaHyvaksyntaaAvoin() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI, VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, vastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA.name))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    fun getValmistumispyynnotWithoutTehtavatDefined() {
        initTest(listOf())
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaOsaamisenArviointia() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByOsaamisenArvioija() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOsaamisenArviointiPalautettu(opintooikeus, anotherVastuuhenkilo)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaTilaPalautettuByOsaamisenArvioija() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOsaamisenArviointiPalautettu(opintooikeus, vastuuhenkilo, palautusaikaMinusDay1)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(VASTUUHENKILON_TARKASTUS_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaikaMinusDay1.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaVirkailijanTarkastusta() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(opintooikeus, vastuuhenkilo)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(ODOTTAA_VIRKAILIJAN_TARKASTUSTA.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaVirkailijanTarkastusta() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(opintooikeus, anotherVastuuhenkilo)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByVirkailija() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoVirkailijanTarkastusPalautettu(opintooikeus, virkailija)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaTilaPalautettuByVirkailija() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoVirkailijanTarkastusPalautettu(opintooikeus, virkailija, palautusaikaMinusDay1)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(VIRKAILIJAN_TARKASTUS_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaikaMinusDay1.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaHyvaksyntaa() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, vastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaTilaPalautettuByHyvaksyja() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(opintooikeus, vastuuhenkilo, palautusaikaMinusDay1)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",)).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaikaMinusDay1.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForOsaamisenArvioijaHyvaksyjaTilaPalautettuByHyvaksyja() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI, VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(opintooikeus, vastuuhenkilo, palautusaikaMinusDay1)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaikaMinusDay1.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA.name))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByHyvaksyja() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(opintooikeus, anotherVastuuhenkilo, palautusaikaMinusDay1)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaikaMinusDay1.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_HYVAKSYJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByHyvaksyjaAndVirkailija() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val virkailijaPalautusaika = LocalDate.now().minusDays(2)
        val hyvaksyjaPalautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaAndTarkastusPalautettu(opintooikeus, virkailija, virkailijaPalautusaika, vastuuhenkilo,
            hyvaksyjaPalautusaika)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(hyvaksyjaPalautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_HYVAKSYJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaTilaPalautettuByHyvaksyjaVirkailijaAndOsaamisenArvioija() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val osaamisenArviointiPalautusaika = LocalDate.now().minusDays(3)
        val virkailijaPalautusaika = LocalDate.now().minusDays(2)
        val hyvaksyjaPalautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaTarkastusAndOsaamisenArviointiPalautettu(opintooikeus, anotherVastuuhenkilo,
                osaamisenArviointiPalautusaika, virkailija, virkailijaPalautusaika, vastuuhenkilo, hyvaksyjaPalautusaika)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(hyvaksyjaPalautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_HYVAKSYJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForHyvaksyjaTilaAllekirjoitettu() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksytty(opintooikeus, anotherVastuuhenkilo, virkailija, vastuuhenkilo)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(HYVAKSYTTY.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_OSAAMISEN_ARVIOIJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyynnotForArvioijaHyvaksyjaTilaHyvaksytty() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksytty(opintooikeus, vastuuhenkilo, virkailija, vastuuhenkilo)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id)).andExpect(jsonPath("$.content[0].tila").value(HYVAKSYTTY.name))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(VASTUUHENKILO_HYVAKSYJA.name)).andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    fun getValmistumispyyntoForOsaamisenArvioijaTilaOdottaaOsaamisenArviointia() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.tila").value(ODOTTAA_VASTUUHENKILON_TARKASTUSTA.name))
            .andExpect(jsonPath("$.muokkauspaiva").value(valmistumispyynto.erikoistujanKuittausaika.toString()))
            .andExpect(jsonPath("$.erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistujanAvatar").value(KayttajaHelper.DEFAULT_AVATAR_AS_STRING))
            .andExpect(jsonPath("$.erikoistujanOpiskelijatunnus").value(valmistumispyynto.opintooikeus?.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistujanSyntymaaika").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.erikoistujanYliopisto").value(valmistumispyynto.opintooikeus?.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.erikoistujanErikoisala").value(valmistumispyynto.opintooikeus?.erikoisala?.nimi))
            .andExpect(jsonPath("$.erikoistujanLaillistamispaiva").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaiva.toString()))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistus").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistusNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistusTyyppi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi))
            .andExpect(jsonPath("$.erikoistujanAsetus").value(valmistumispyynto.opintooikeus?.asetus?.nimi))
            .andExpect(jsonPath("$.opintooikeusId").value(valmistumispyynto.opintooikeus?.id))
            .andExpect(jsonPath("$.opintooikeudenMyontamispaiva").value(valmistumispyynto.opintooikeus?.opintooikeudenMyontamispaiva.toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimi").isEmpty).andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimike").isEmpty)
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").isEmpty)
    }

    @Test
    fun tryToGetValmistumispyyntoFromDifferentYliopisto() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)
        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().is5xxServerError)
    }

    @Test
    fun tryToGetValmistumispyyntoFromDifferentErikoisala() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val anotherErikoisala = ErikoisalaHelper.createEntity()
        em.persist(anotherErikoisala)
        val erikoistuvaLaakari = initErikoistuvaLaakari(opintooikeus.yliopisto, anotherErikoisala)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().is5xxServerError)
    }

    @Test
    fun getValmistumispyyntoArviointienTilaHasNotArvioitaviaKokonaisuuksiaWithoutArviointiOrLowerThanFour() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)
        val arvKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvKokonaisuus)
        val arv = SuoritusarviointiHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user, arviointiasteikonTaso = 4, arvioitavaKokonaisuus = arvKokonaisuus)
        em.persist(arv)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(false))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(false))
    }

    @Test
    fun getValmistumispyyntoArviointienTilaHasArvioitaviaKokonaisuuksiaWithoutArviointi() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)
        val arvKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvKokonaisuus)
        val arvKokonaisuus2 = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvKokonaisuus2)
        val arv = SuoritusarviointiHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user, arviointiasteikonTaso = 4, arvioitavaKokonaisuus = arvKokonaisuus)
        em.persist(arv)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(false))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(true))
    }

    @Test
    fun getValmistumispyyntoArviointienTilaHasArviointiAsteikonTasoLowerThanFour() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)
        val arvKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvKokonaisuus)
        val arv = SuoritusarviointiHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user, arviointiasteikonTaso = 3, arvioitavaKokonaisuus = arvKokonaisuus)
        em.persist(arv)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(true))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(false))
    }

    @Test
    fun getValmistumispyyntoArviointienTilaHasArvioitaviaKokonaisuuksiaWithoutArviointiAndArviointiAsteikonTasoLowerThanFour() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)
        val arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus)
        val arvioitavaKokonaisuus2 = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus2)
        val arviointi = SuoritusarviointiHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user, arviointiasteikonTaso = 3,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus)
        em.persist(arviointi)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(true))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(true))
    }

    @Test
    fun tryToGetArviointienTilaDifferentYliopisto() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)
        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().is5xxServerError)
    }

    @Test
    fun tryToGetArviointienTilaDifferentErikoisala() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val anotherErikoisala = ErikoisalaHelper.createEntity()
        em.persist(anotherErikoisala)
        val erikoistuvaLaakari = initErikoistuvaLaakari(opintooikeus.yliopisto, anotherErikoisala)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().is5xxServerError)
    }

    @Test
    fun getValmistumispyyntoForOsaamisenArvioijaTilaArvioitu() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(opintooikeus, vastuuhenkilo)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.tila").value(ODOTTAA_VIRKAILIJAN_TARKASTUSTA.name))
            .andExpect(jsonPath("$.muokkauspaiva").value(valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika.toString()))
            .andExpect(jsonPath("$.erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistujanAvatar").value(KayttajaHelper.DEFAULT_AVATAR_AS_STRING))
            .andExpect(jsonPath("$.erikoistujanOpiskelijatunnus").value(valmistumispyynto.opintooikeus?.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistujanSyntymaaika").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.erikoistujanYliopisto").value(valmistumispyynto.opintooikeus?.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.erikoistujanErikoisala").value(valmistumispyynto.opintooikeus?.erikoisala?.nimi))
            .andExpect(jsonPath("$.erikoistujanLaillistamispaiva").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaiva.toString()))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistus").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistusNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistusTyyppi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi))
            .andExpect(jsonPath("$.erikoistujanAsetus").value(valmistumispyynto.opintooikeus?.asetus?.nimi))
            .andExpect(jsonPath("$.opintooikeusId").value(valmistumispyynto.opintooikeus?.id))
            .andExpect(jsonPath("$.opintooikeudenMyontamispaiva").value(valmistumispyynto.opintooikeus?.opintooikeudenMyontamispaiva.toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimi").value(valmistumispyynto.vastuuhenkiloOsaamisenArvioija?.getNimi()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimike").value(valmistumispyynto.vastuuhenkiloOsaamisenArvioija?.nimike))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").value(valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika.toString()))
    }

    @Test
    fun getValmistumispyyntoForHyvaksyjaTilaOdottaaOsaamisenArviointia() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().is5xxServerError)
    }

    @Test
    fun getValmistumispyyntoWithoutTehtavatDefined() {
        initTest(listOf())
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        testMockMvc.perform(get("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().is5xxServerError)
    }

    @Test
    fun ackValmistumispyyntoOsaamisenArviointi() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(ValmistumispyyntoOsaamisenArviointiFormDTO(true))).with(csrf())).andExpect(status().isOk)
        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioija).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isNull()
    }

    @Test
    fun tryToAckValmistumispyyntoWithoutTehtavatDefined() {
        initTest(listOf())
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(true)
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().is5xxServerError)
    }

    @Test
    fun tryToAckValmistumispyyntoFromDifferentYliopisto() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)
        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)
        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(true)
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().is5xxServerError)
    }

    @Test
    fun tryToAckValmistumispyyntoFromDifferentErikoisala() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val anotherErikoisala = ErikoisalaHelper.createEntity()
        em.persist(anotherErikoisala)
        val erikoistuvaLaakari = initErikoistuvaLaakari(opintooikeus.yliopisto, anotherErikoisala)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)
        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(true)
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().is5xxServerError)
    }

    @Test
    fun declineValmistumispyyntoOsaamisenArviointi() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size
        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(false, "korjausehdotus")
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().isOk)
        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioija).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isEqualTo("korjausehdotus")
    }

    @Test
    fun declineValmistumispyyntoOsaamisenArviointiWithoutKorjausehdotus() {
        initTest(listOf(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)
        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(false)
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun getValmistumispyyntoForVirkailijaTilaOdottaaTarkistusta() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)
        val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)
        val hyvaksyntaPvm = LocalDate.ofEpochDay(12)
        val terveyskeskustyoHyvaksynta = TerveyskeskustyoHelper.createTerveyskeskustyoHyvaksyntaHyvaksytty(opintooikeus, hyvaksyntaPvm)
        em.persist(terveyskeskustyoHyvaksynta)
        val terveyssuoritus = OpintosuoritusHelper.createEntity(em, tyyppiEnum = OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO)
        em.persist(terveyssuoritus)
        val teoriakoulutus1 = TeoriakoulutusHelper.createEntity(em, user = erikoistuvaLaakari.kayttaja?.user)
        em.persist(teoriakoulutus1)
        val teoriakoulutus2 = TeoriakoulutusHelper.createEntity(em, user = erikoistuvaLaakari.kayttaja?.user)
        em.persist(teoriakoulutus2)
        val sateilysuojakoulutus = OpintosuoritusHelper.createEntity(em, tyyppiEnum = OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS)
        em.persist(sateilysuojakoulutus)
        val johtamiskoulutus = OpintosuoritusHelper.createEntity(em, tyyppiEnum = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO)
        em.persist(johtamiskoulutus)
        em.persist(OpintosuoritusHelper.createEntity(em, tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU))
        em.persist(OpintosuoritusHelper.createEntity(em, tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU))
        val vastuuhenkilonArvio = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
        vastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = true
        vastuuhenkilonArvio.vastuuhenkilonKuittausaika = LocalDate.ofEpochDay(20)
        em.persist(vastuuhenkilonArvio)
        testMockMvc.perform(get("${ENDPOINT_BASE_URL}$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/${valmistumispyynto.id}")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").value(tarkistus.id))
            .andExpect(jsonPath("$.valmistumispyynto.tila").value(ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$.valmistumispyynto.muokkauspaiva").value(valmistumispyynto.virkailijanKuittausaika.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanAvatar").value(KayttajaHelper.DEFAULT_AVATAR_AS_STRING))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanOpiskelijatunnus").value(valmistumispyynto.opintooikeus?.opiskelijatunnus))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanSyntymaaika").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanYliopisto").value(valmistumispyynto.opintooikeus?.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanErikoisala").value(valmistumispyynto.opintooikeus?.erikoisala?.nimi))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanLaillistamispaiva").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaiva.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanLaillistamistodistus").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanLaillistamistodistusNimi").value(
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanLaillistamistodistusTyyppi").value(
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanAsetus").value(valmistumispyynto.opintooikeus?.asetus?.nimi))
            .andExpect(jsonPath("$.valmistumispyynto.opintooikeusId").value(valmistumispyynto.opintooikeus?.id))
            .andExpect(jsonPath("$.valmistumispyynto.opintooikeudenMyontamispaiva").value(valmistumispyynto.opintooikeus?.opintooikeudenMyontamispaiva.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.vastuuhenkiloOsaamisenArvioijaNimi").value(vastuuhenkilo.getNimi()))
            .andExpect(jsonPath("$.valmistumispyynto.vastuuhenkiloOsaamisenArvioijaNimike").value(vastuuhenkilo.nimike))
            .andExpect(jsonPath("$.valmistumispyynto.virkailijaNimi").value(virkailija.getNimi()))
            .andExpect(jsonPath("$.terveyskeskustyoHyvaksyttyPvm").value(hyvaksyntaPvm.toString()))
            .andExpect(jsonPath("$.terveyskeskustyoHyvaksyntaId").value(terveyskeskustyoHyvaksynta.id))
            .andExpect(jsonPath("$.terveyskeskustyoOpintosuoritusId").value(terveyssuoritus.id))
            .andExpect(jsonPath("$.teoriakoulutusSuoritettu").value(teoriakoulutus1.erikoistumiseenHyvaksyttavaTuntimaara?.plus(
                        teoriakoulutus2.erikoistumiseenHyvaksyttavaTuntimaara!!)))
            .andExpect(jsonPath("$.teoriakoulutusVaadittu").value(opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara))
            .andExpect(jsonPath("$.sateilusuojakoulutusSuoritettu").value(sateilysuojakoulutus.opintopisteet))
            .andExpect(jsonPath("$.sateilusuojakoulutusVaadittu").value(opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara))
            .andExpect(jsonPath("$.johtamiskoulutusSuoritettu").value(johtamiskoulutus.opintopisteet))
            .andExpect(jsonPath("$.johtamiskoulutusVaadittu").value(opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara))
            .andExpect(jsonPath("$.kuulustelut").value(Matchers.hasSize<Any>(2))).andExpect(jsonPath("$.koejaksoHyvaksyttyPvm").value(LocalDate.ofEpochDay(20).toString()))
    }

    @Test
    fun ackValmistumispyyntoHyvaksyja() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)
        em.persist(ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto))
        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size
        val valmistumispyyntoFormDTO = ValmistumispyyntoHyvaksyntaFormDTO(null)
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().isOk)
        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyja).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus).isNull()
    }

    @Test
    fun declineValmistumispyyntoHyvaksyja() {
        initTest(listOf(VALMISTUMISPYYNNON_HYVAKSYNTA))
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)
        val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)
        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size
        val valmistumispyyntoFormDTO = ValmistumispyyntoHyvaksyntaFormDTO("korjausehdotus")
        testMockMvc.perform(put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(valmistumispyyntoFormDTO)).with(csrf())).andExpect(status().isOk)
        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyja).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus).isEqualTo("korjausehdotus")
    }

    fun initTest(vastuuhenkilonTehtavatyypit: List<VastuuhenkilonTehtavatyyppiEnum>) {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(vastuuhenkiloUser)
        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(DefaultSaml2AuthenticatedPrincipal(vastuuhenkiloUser.id, mapOf<String, List<Any>>()), "test", authorities)
        TestSecurityContextHolder.getContext().authentication = authentication
        erikoistuvaLaakari = initErikoistuvaLaakari()
        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!
        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)
        vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        val tehtavat = tehtavatyypit.filter { it.nimi in vastuuhenkilonTehtavatyypit }.toMutableSet()
        initVastuuhenkiloErikoisalat(vastuuhenkilo, opintooikeus.yliopisto!!, opintooikeus.erikoisala!!, tehtavat)
        em.persist(vastuuhenkilo)
        if (!vastuuhenkilonTehtavatyypit.contains(VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI) || !vastuuhenkilonTehtavatyypit.contains(VALMISTUMISPYYNNON_HYVAKSYNTA)) {
            val anotherVastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
            em.persist(anotherVastuuhenkiloUser)
            anotherVastuuhenkilo = KayttajaHelper.createEntity(em, anotherVastuuhenkiloUser)
            val anotherVastuuhenkiloTehtavat = tehtavatyypit.filter { it.nimi !in vastuuhenkilonTehtavatyypit }.toMutableSet()
            initVastuuhenkiloErikoisalat(anotherVastuuhenkilo, opintooikeus.yliopisto!!, opintooikeus.erikoisala!!, anotherVastuuhenkiloTehtavat)
            em.persist(anotherVastuuhenkilo)
        }
        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA))
        em.persist(virkailijaUser)
        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        em.persist(virkailija)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
    }

    private fun initVastuuhenkiloErikoisalat(user: Kayttaja, yliopisto: Yliopisto, erikoisala: Erikoisala, tehtavat: MutableSet<VastuuhenkilonTehtavatyyppi>) {
        val newErikoisala = ErikoisalaHelper.createEntity()
        val otherNewErikoisala = ErikoisalaHelper.createEntity()
        em.persist(newErikoisala)
        em.persist(otherNewErikoisala)
        user.yliopistotAndErikoisalat.add(KayttajaYliopistoErikoisala(kayttaja = user, yliopisto = yliopisto, erikoisala = newErikoisala, vastuuhenkilonTehtavat = tehtavat))
        user.yliopistotAndErikoisalat.add(KayttajaYliopistoErikoisala(kayttaja = user, yliopisto = yliopisto, erikoisala = erikoisala, vastuuhenkilonTehtavat = tehtavat))
        user.yliopistotAndErikoisalat.add(KayttajaYliopistoErikoisala(kayttaja = user, yliopisto = yliopisto, erikoisala = otherNewErikoisala, vastuuhenkilonTehtavat = tehtavat))
    }
}
