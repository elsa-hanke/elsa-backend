package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoOsaamisenArviointiFormDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
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
import java.time.LocalDateTime
import jakarta.persistence.EntityManager

private const val ENDPOINT_BASE_URL = "/api/vastuuhenkilo"
private const val VALMISTUMISPYYNNOT_ENDPOINT = "/valmistumispyynnot?page=0&size=20&sort=muokkauspaiva,desc"
private const val VALMISTUMISPYYNNON_HYVAKSYJA_ROLE_ENDPOINT = "/valmistumispyynnon-hyvaksyja-role"
private const val VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT = "/valmistumispyynnon-arviointi"
private const val VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT = "/valmistumispyynto-arviointien-tila"
private const val VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT = "/valmistumispyynnon-hyvaksynta"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VastuuhenkiloValmistumispyyntoResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository


    @Autowired
    private lateinit var restValmistumispyyntoMockMvc: MockMvc

    private lateinit var opintooikeus: Opintooikeus

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var vastuuhenkilo: Kayttaja

    private lateinit var virkailija: Kayttaja

    private lateinit var anotherVastuuhenkilo: Kayttaja

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaOsaamisenArviointiaAvoin() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaHyvaksyntaaAvoin() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            opintooikeus,
            anotherVastuuhenkilo,
            virkailija
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaHyvaksyjaTilaOdottaaHyvaksyntaaAvoin() {
        initTest(
            listOf(
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI,
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            )
        )

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            opintooikeus,
            vastuuhenkilo,
            virkailija
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotWithoutTehtavatDefined() {
        initTest(listOf())

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaOsaamisenArviointia() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(
            opintooikeus
        )
        em.persist(valmistumispyynto)


        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByOsaamisenArvioija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOsaamisenArviointiPalautettu(
            opintooikeus,
            anotherVastuuhenkilo
        )
        em.persist(valmistumispyynto)


        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaPalautettuByOsaamisenArvioija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOsaamisenArviointiPalautettu(
            opintooikeus,
            vastuuhenkilo,
            palautusaika
        )
        em.persist(valmistumispyynto)


        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaVirkailijanTarkastusta() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
            opintooikeus,
            vastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaVirkailijanTarkastusta() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
            opintooikeus,
            anotherVastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByVirkailija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoVirkailijanTarkastusPalautettu(
            opintooikeus,
            virkailija
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaPalautettuByVirkailija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoVirkailijanTarkastusPalautettu(
            opintooikeus,
            virkailija,
            palautusaika
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaHyvaksyntaa() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            opintooikeus,
            vastuuhenkilo,
            virkailija
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaPalautettuByHyvaksyja() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(
            opintooikeus,
            vastuuhenkilo,
            palautusaika
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaHyvaksyjaTilaPalautettuByHyvaksyja() {
        initTest(
            listOf(
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI,
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            )
        )

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(
            opintooikeus,
            vastuuhenkilo,
            palautusaika
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByHyvaksyja() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(
            opintooikeus,
            anotherVastuuhenkilo,
            palautusaika
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(palautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaPalautettuByHyvaksyjaAndVirkailija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val virkailijaPalautusaika = LocalDate.now().minusDays(2)
        val hyvaksyjaPalautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaAndTarkastusPalautettu(
            opintooikeus,
            virkailija,
            virkailijaPalautusaika,
            vastuuhenkilo,
            hyvaksyjaPalautusaika
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(hyvaksyjaPalautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaTilaPalautettuByHyvaksyjaVirkailijaAndOsaamisenArvioija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val osaamisenArviointiPalautusaika = LocalDate.now().minusDays(3)
        val virkailijaPalautusaika = LocalDate.now().minusDays(2)
        val hyvaksyjaPalautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaTarkastusAndOsaamisenArviointiPalautettu(
                opintooikeus,
                anotherVastuuhenkilo,
                osaamisenArviointiPalautusaika,
                virkailija,
                virkailijaPalautusaika,
                vastuuhenkilo,
                hyvaksyjaPalautusaika

            )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(hyvaksyjaPalautusaika.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaAllekirjoitettu() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksytty(
            opintooikeus,
            anotherVastuuhenkilo,
            virkailija,
            vastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.HYVAKSYTTY.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForArvioijaHyvaksyjaTilaHyvaksytty() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksytty(
            opintooikeus,
            vastuuhenkilo,
            virkailija,
            vastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.HYVAKSYTTY.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].rooli").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoForOsaamisenArvioijaTilaOdottaaOsaamisenArviointia() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA.toString()))
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
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimi").isEmpty)
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimike").isEmpty)
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").isEmpty)
    }

    @Test
    @Transactional
    fun tryToGetValmistumispyyntoFromDifferentYliopisto() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)
        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun tryToGetValmistumispyyntoFromDifferentErikoisala() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val anotherErikoisala = ErikoisalaHelper.createEntity()
        em.persist(anotherErikoisala)
        val erikoistuvaLaakari = initErikoistuvaLaakari(opintooikeus.yliopisto, anotherErikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun getValmistumispyyntoArviointienTilaHasNotArvioitaviaKokonaisuuksiaWithoutArviointiOrLowerThanFour() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)

        val arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus)

        val arviointi = SuoritusarviointiHelper.createEntity(
            em,
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user,
            arviointiasteikonTaso = 4,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus
        )
        em.persist(arviointi)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(false))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoArviointienTilaHasArvioitaviaKokonaisuuksiaWithoutArviointi() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)

        val arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus)

        val arvioitavaKokonaisuus2 = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus2)

        val arviointi = SuoritusarviointiHelper.createEntity(
            em,
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user,
            arviointiasteikonTaso = 4,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus
        )
        em.persist(arviointi)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(false))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoArviointienTilaHasArviointiAsteikonTasoLowerThanFour() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)

        val arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus)

        val arviointi = SuoritusarviointiHelper.createEntity(
            em,
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user,
            arviointiasteikonTaso = 3,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus
        )
        em.persist(arviointi)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(true))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoArviointienTilaHasArvioitaviaKokonaisuuksiaWithoutArviointiAndArviointiAsteikonTasoLowerThanFour() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, opintooikeus.erikoistuvaLaakari?.kayttaja?.user)
        em.persist(tyoskentelyjakso)

        val arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus)

        val arvioitavaKokonaisuus2 = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus2)

        val arviointi = SuoritusarviointiHelper.createEntity(
            em,
            opintooikeus.erikoistuvaLaakari?.kayttaja?.user,
            arviointiasteikonTaso = 3,
            arvioitavaKokonaisuus = arvioitavaKokonaisuus
        )
        em.persist(arviointi)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour").value(true))
            .andExpect(jsonPath("$.hasArvioitaviaKokonaisuuksiaWithoutArviointi").value(true))
    }

    @Test
    @Transactional
    fun tryToGetArviointienTilaDifferentYliopisto() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)

        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun tryToGetArviointienTilaDifferentErikoisala() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val anotherErikoisala = ErikoisalaHelper.createEntity()
        em.persist(anotherErikoisala)

        val erikoistuvaLaakari = initErikoistuvaLaakari(opintooikeus.yliopisto, anotherErikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNTO_ARVIOINTIEN_TILA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun getValmistumispyyntoForOsaamisenArvioijaTilaArvioitu() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
            opintooikeus,
            vastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA.toString()))
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
    @Transactional
    fun getValmistumispyyntoForHyvaksyjaTilaOdottaaOsaamisenArviointia() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"
            )
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun getValmistumispyyntoWithoutTehtavatDefined() {
        initTest(listOf())

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/${valmistumispyynto.id}"
            )
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun ackValmistumispyyntoOsaamisenArviointi() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size

        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(
            true
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioija).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isNull()
    }

    @Test
    @Transactional
    fun tryToAckValmistumispyyntoWithoutTehtavatDefined() {
        initTest(listOf())

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(
            true
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun tryToAckValmistumispyyntoFromDifferentYliopisto() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)

        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)

        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(
            true
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun tryToAckValmistumispyyntoFromDifferentErikoisala() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val anotherErikoisala = ErikoisalaHelper.createEntity()
        em.persist(anotherErikoisala)
        val erikoistuvaLaakari = initErikoistuvaLaakari(opintooikeus.yliopisto, anotherErikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(erikoistuvaLaakari.getOpintooikeusKaytossa()!!)
        em.persist(valmistumispyynto)

        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(
            true
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun declineValmistumispyyntoOsaamisenArviointi() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size

        val korjausehdotus = "korjausehdotus"
        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(
            false,
            korjausehdotus
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioija).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isEqualTo(korjausehdotus)
    }

    @Test
    @Transactional
    fun declineValmistumispyyntoOsaamisenArviointiWithoutKorjausehdotus() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        val valmistumispyyntoFormDTO = ValmistumispyyntoOsaamisenArviointiFormDTO(
            false
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_ARVIOINTI_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getValmistumispyyntoForVirkailijaTilaOdottaaTarkistusta() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)

        val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)

        val hyvaksyntaPvm = LocalDate.ofEpochDay(12)
        val terveyskeskustyoHyvaksynta =
            TerveyskeskustyoHelper.createTerveyskeskustyoHyvaksyntaHyvaksytty(
                opintooikeus,
                hyvaksyntaPvm
            )
        em.persist(terveyskeskustyoHyvaksynta)

        val terveyssuoritus = OpintosuoritusHelper.createEntity(
            em,
            tyyppiEnum = OpintosuoritusTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO
        )
        em.persist(terveyssuoritus)

        val teoriakoulutus1 =
            TeoriakoulutusHelper.createEntity(em, user = erikoistuvaLaakari.kayttaja?.user)
        em.persist(teoriakoulutus1)

        val teoriakoulutus2 =
            TeoriakoulutusHelper.createEntity(em, user = erikoistuvaLaakari.kayttaja?.user)
        em.persist(teoriakoulutus2)

        val sateilysuojakoulutus = OpintosuoritusHelper.createEntity(
            em,
            tyyppiEnum = OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS
        )
        em.persist(sateilysuojakoulutus)

        val johtamiskoulutus = OpintosuoritusHelper.createEntity(
            em,
            tyyppiEnum = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO
        )
        em.persist(johtamiskoulutus)

        val kuulustelu1 = OpintosuoritusHelper.createEntity(
            em,
            tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU
        )
        em.persist(kuulustelu1)

        val kuulustelu2 = OpintosuoritusHelper.createEntity(
            em,
            tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU
        )
        em.persist(kuulustelu2)

        val koejaksoHyvaksyttyPvm = LocalDate.ofEpochDay(20)
        val vastuuhenkilonArvio =
            KoejaksonVaiheetHelper.createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
        vastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = true
        vastuuhenkilonArvio.vastuuhenkilonKuittausaika = koejaksoHyvaksyttyPvm
        em.persist(vastuuhenkilonArvio)

        restValmistumispyyntoMockMvc.perform(
            get(
                "${ENDPOINT_BASE_URL}$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tarkistus.id))
            .andExpect(jsonPath("$.valmistumispyynto.tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.muokkauspaiva").value(valmistumispyynto.virkailijanKuittausaika.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanAvatar").value(KayttajaHelper.DEFAULT_AVATAR_AS_STRING))
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanOpiskelijatunnus").value(
                    valmistumispyynto.opintooikeus?.opiskelijatunnus
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanSyntymaaika").value(
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.syntymaaika.toString()
                )
            )
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanYliopisto").value(valmistumispyynto.opintooikeus?.yliopisto?.nimi.toString()))
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanErikoisala").value(
                    valmistumispyynto.opintooikeus?.erikoisala?.nimi
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanLaillistamispaiva").value(
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaiva.toString()
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanLaillistamistodistus").value(
                    ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanLaillistamistodistusNimi").value(
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.erikoistujanLaillistamistodistusTyyppi").value(
                    valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi
                )
            )
            .andExpect(jsonPath("$.valmistumispyynto.erikoistujanAsetus").value(valmistumispyynto.opintooikeus?.asetus?.nimi))
            .andExpect(jsonPath("$.valmistumispyynto.opintooikeusId").value(valmistumispyynto.opintooikeus?.id))
            .andExpect(
                jsonPath("$.valmistumispyynto.opintooikeudenMyontamispaiva").value(
                    valmistumispyynto.opintooikeus?.opintooikeudenMyontamispaiva.toString()
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.vastuuhenkiloOsaamisenArvioijaNimi").value(
                    vastuuhenkilo.getNimi()
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.vastuuhenkiloOsaamisenArvioijaNimike").value(
                    vastuuhenkilo.nimike
                )
            )
            .andExpect(
                jsonPath("$.valmistumispyynto.virkailijaNimi").value(
                    virkailija.getNimi()
                )
            )
            .andExpect(
                jsonPath("$.terveyskeskustyoHyvaksyttyPvm").value(hyvaksyntaPvm.toString())
            )
            .andExpect(
                jsonPath("$.terveyskeskustyoHyvaksyntaId").value(terveyskeskustyoHyvaksynta.id)
            )
            .andExpect(
                jsonPath("$.terveyskeskustyoOpintosuoritusId").value(terveyssuoritus.id)
            )
            .andExpect(
                jsonPath("$.teoriakoulutusSuoritettu").value(
                    teoriakoulutus1.erikoistumiseenHyvaksyttavaTuntimaara?.plus(
                        teoriakoulutus2.erikoistumiseenHyvaksyttavaTuntimaara!!
                    )
                )
            )
            .andExpect(
                jsonPath("$.teoriakoulutusVaadittu").value(opintooikeus.opintoopas?.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara)
            )
            .andExpect(
                jsonPath("$.sateilusuojakoulutusSuoritettu").value(sateilysuojakoulutus.opintopisteet)
            )
            .andExpect(
                jsonPath("$.sateilusuojakoulutusVaadittu").value(opintooikeus.opintoopas?.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara)
            )
            .andExpect(
                jsonPath("$.johtamiskoulutusSuoritettu").value(johtamiskoulutus.opintopisteet)
            )
            .andExpect(
                jsonPath("$.johtamiskoulutusVaadittu").value(opintooikeus.opintoopas?.erikoisalanVaatimaJohtamisopintojenVahimmaismaara)
            )
            .andExpect(
                jsonPath("$.kuulustelut").value(Matchers.hasSize<Any>(2))
            )
            .andExpect(
                jsonPath("$.koejaksoHyvaksyttyPvm").value(koejaksoHyvaksyttyPvm.toString())
            )
    }

    @Test
    @Transactional
    fun ackValmistumispyyntoHyvaksyja() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)

        val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)

        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size

        val valmistumispyyntoFormDTO = ValmistumispyyntoHyvaksyntaFormDTO(
            null
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyja).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus).isNull()
    }

    /**
     * Regression test for the production bug: a suoritusarviointi with a zero-byte PDF attachment
     * (asiakirjaData.data = ByteArray(0)) causes PdfReader to throw
     * "com.itextpdf.io.exceptions.IOException: PDF header not found" inside lisaaArvioinnit,
     * which propagates through the @Transactional boundary and rolls back the entire approval.
     *
     * This test CURRENTLY FAILS with 5xx until the fix (skip/warn on empty blobs) is applied.
     * Once the fix is in place, update the assertion to expect status().isOk.
     */
    @Test
    @Transactional
    fun ackValmistumispyyntoHyvaksyjaWithEmptyPdfAttachmentFails() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        // Create a tyoskentelyjakso directly linked to the test's opintooikeus.
        // TyoskentelyjaksoHelper sets the opintooikeus via getOpintooikeusKaytossa() which
        // can resolve to a different object; we override it explicitly to guarantee the link.
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em)
        tyoskentelyjakso.opintooikeus = opintooikeus
        em.persist(tyoskentelyjakso)
        em.flush()

        // SuoritusarviointiHelper.createEntity picks up the persisted tyoskentelyjakso via
        // em.findAll(Tyoskentelyjakso).get(0) and sets the correct opintooikeus chain.
        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em)
        em.persist(suoritusarviointi)
        em.flush()

        // Attach a zero-byte PDF – this is the exact data shape found in production
        // (asiakirja_data records with LENGTH(data) = 0 linked to suoritusarviointi).
        // PdfReader receives an empty ByteArrayInputStream and throws "PDF header not found".
        val emptyPdfAsiakirja = Asiakirja(
            opintooikeus = opintooikeus,
            arviointi = suoritusarviointi,
            nimi = "empty.pdf",
            tyyppi = MediaType.APPLICATION_PDF_VALUE,
            lisattypvm = LocalDateTime.now(),
            asiakirjaData = AsiakirjaData(data = ByteArray(0))
        )
        em.persist(emptyPdfAsiakirja)
        em.flush()

        // Create a Koulutussuunnitelma for the opintooikeus so that lisaaKoulutussuunnitelma
        // can render the PDF template without crashing.
        // Without this, koulutussuunnitelmaRepository.findOneByOpintooikeusId returns null and
        // the Thymeleaf template crashes at `koulutussuunnitelma.koulutusjaksot` (null dereference)
        // before we even reach lisaaArvioinnit where the actual production error occurs.
        val koulutussuunnitelma = Koulutussuunnitelma(
            opintooikeus = opintooikeus,
            motivaatiokirjeYksityinen = false,
            opiskeluJaTyohistoriaYksityinen = false,
            vahvuudetYksityinen = false,
            tulevaisuudenVisiointiYksityinen = false,
            osaamisenKartuttaminenYksityinen = false,
            elamankenttaYksityinen = false
        )
        em.persist(koulutussuunnitelma)
        em.flush()

        // Clear the first-level cache so the service loads the suoritusarviointi fresh from DB.
        // Without this, the repository JPQL query returns the entity from the first-level cache
        // whose arviointiAsiakirjat is still the original empty mutableSetOf() (a plain Kotlin set,
        // not a Hibernate lazy proxy), so the forEach is a no-op and no crash occurs.
        // After em.clear(), a.arviointiAsiakirjat triggers a lazy load from DB, finds the
        // zero-byte asiakirja, and PdfReader throws "PDF header not found".
        em.clear()

        // Re-fetch detached entities by ID for the valmistumispyynto setup.
        val freshOpintooikeus = em.find(Opintooikeus::class.java, opintooikeus.id!!)
        val freshAnotherVastuuhenkilo = em.find(Kayttaja::class.java, anotherVastuuhenkilo.id!!)
        val freshVirkailija = em.find(Kayttaja::class.java, virkailija.id!!)

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            freshOpintooikeus, freshAnotherVastuuhenkilo, freshVirkailija
        )
        em.persist(valmistumispyynto)

        val tarkistus = ValmistumispyynnonTarkistusHelper
            .createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)
        // Set the non-owning back-reference so the cached valmistumispyynto entity knows about
        // its tarkistus. Without this, result.valmistumispyynnonTarkistus is null in the
        // first-level-cache copy and the let{} block (containing luoErikoistujanTiedotPdf)
        // is never entered, returning 200 without ever touching the zero-byte PDF attachment.
        valmistumispyynto.valmistumispyynnonTarkistus = tarkistus
        em.flush()

        val valmistumispyyntoFormDTO = ValmistumispyyntoHyvaksyntaFormDTO(null)

        // TODO: after fixing lisaaArvioinnit to skip/warn on empty blobs,
        //       change this assertion to .andExpect(status().isOk)
        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun declineValmistumispyyntoHyvaksyja() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(opintooikeus, anotherVastuuhenkilo, virkailija)
        em.persist(valmistumispyynto)

        val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)

        val databaseSizeBeforeUpdate = valmistumispyyntoRepository.findAll().size

        val korjausehdotus = "korjausehdotus"
        val valmistumispyyntoFormDTO = ValmistumispyyntoHyvaksyntaFormDTO(
            korjausehdotus
        )

        restValmistumispyyntoMockMvc.perform(
            put("$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyyntoFormDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyja).isEqualTo(vastuuhenkilo)
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika).isNull()
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.vastuuhenkiloHyvaksyjaKorjausehdotus).isEqualTo(korjausehdotus)
    }

    fun initTest(vastuuhenkilonTehtavatyypit: List<VastuuhenkilonTehtavatyyppiEnum>) {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(vastuuhenkiloUser)
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(vastuuhenkiloUser.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        erikoistuvaLaakari = initErikoistuvaLaakari()

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)
        vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        val tehtavat = tehtavatyypit.filter { it.nimi in vastuuhenkilonTehtavatyypit }.toMutableSet()
        initVastuuhenkiloErikoisalat(vastuuhenkilo, opintooikeus.yliopisto!!, opintooikeus.erikoisala!!, tehtavat)
        em.persist(vastuuhenkilo)

        if (!vastuuhenkilonTehtavatyypit.contains(
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
            ) || !vastuuhenkilonTehtavatyypit.contains(
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            )
        ) {
            val anotherVastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
                authority = Authority(
                    VASTUUHENKILO
                )
            )
            em.persist(anotherVastuuhenkiloUser)

            anotherVastuuhenkilo = KayttajaHelper.createEntity(em, anotherVastuuhenkiloUser)
            val anotherVastuuhenkiloTehtavat =
                tehtavatyypit.filter { it.nimi !in vastuuhenkilonTehtavatyypit }.toMutableSet()
            initVastuuhenkiloErikoisalat(
                anotherVastuuhenkilo,
                opintooikeus.yliopisto!!,
                opintooikeus.erikoisala!!,
                anotherVastuuhenkiloTehtavat
            )
            em.persist(anotherVastuuhenkilo)
        }

        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                OPINTOHALLINNON_VIRKAILIJA
            )
        )
        em.persist(virkailijaUser)

        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        em.persist(virkailija)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
    }

    private fun initVastuuhenkiloErikoisalat(
        kayttaja: Kayttaja,
        yliopisto: Yliopisto,
        erikoisala: Erikoisala,
        tehtavat: MutableSet<VastuuhenkilonTehtavatyyppi>
    ) {
        val newErikoisala = ErikoisalaHelper.createEntity()
        val anotherNewErikoisala = ErikoisalaHelper.createEntity()

        em.persist(newErikoisala)
        em.persist(anotherNewErikoisala)

        kayttaja.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = kayttaja,
                yliopisto = yliopisto,
                erikoisala = newErikoisala,
                vastuuhenkilonTehtavat = tehtavat
            )
        )

        kayttaja.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = kayttaja,
                yliopisto = yliopisto,
                erikoisala = erikoisala,
                vastuuhenkilonTehtavat = tehtavat
            )
        )

        kayttaja.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = kayttaja,
                yliopisto = yliopisto,
                erikoisala = anotherNewErikoisala,
                vastuuhenkilonTehtavat = tehtavat
            )
        )
    }

    private fun initErikoistuvaLaakari(
        yliopisto: Yliopisto? = null,
        erikoisala: Erikoisala? = null
    ): ErikoistuvaLaakari {
        val erikoistuvaLaakariUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                ERIKOISTUVA_LAAKARI
            )
        )
        em.persist(erikoistuvaLaakariUser)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(
            em,
            erikoistuvaLaakariUser,
            yliopisto = yliopisto,
            erikoisala = erikoisala
        )
        em.persist(erikoistuvaLaakari)

        return erikoistuvaLaakari
    }
}
