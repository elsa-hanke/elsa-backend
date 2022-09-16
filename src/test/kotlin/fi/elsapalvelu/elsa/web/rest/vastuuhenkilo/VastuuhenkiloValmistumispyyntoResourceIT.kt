package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoOsaamisenArviointiFormDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonHyvaksyjaRole
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
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
import javax.persistence.EntityManager

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

    private lateinit var vastuuhenkilo: Kayttaja

    private lateinit var virkailija: Kayttaja

    private lateinit var anotherVastuuhenkilo: Kayttaja

    @Test
    @Transactional
    fun getValmistumispyynnonHyvaksyjaRoleOsaamisenArvioija() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNNON_HYVAKSYJA_ROLE_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA.toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyynnonHyvaksyjaRoleHyvaksyja() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNNON_HYVAKSYJA_ROLE_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA.toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyynnonHyvaksyjaRoleArvioijaHyvaksyja() {
        initTest(
            listOf(
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI,
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNNON_HYVAKSYJA_ROLE_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_OSAAMISEN_ARVIOIJA_HYVAKSYJA.toString()))
    }

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
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaOdottaaAllekirjoituksiaAvoin() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaAllekirjoituksia(
            opintooikeus,
            anotherVastuuhenkilo,
            virkailija,
            vastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_ALLEKIRJOITUKSIA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaHyvaksyjaTilaOdottaaAllekirjoituksiaAvoin() {
        initTest(
            listOf(
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI,
                VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            )
        )

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaAllekirjoituksia(
            opintooikeus,
            vastuuhenkilo,
            virkailija,
            vastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL${VALMISTUMISPYYNNOT_ENDPOINT}&avoin=true",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_ALLEKIRJOITUKSIA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
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
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaOdottaaAllekirjoituksia() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaAllekirjoituksia(
            opintooikeus,
            vastuuhenkilo,
            virkailija,
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
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_ALLEKIRJOITUKSIA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForOsaamisenArvioijaTilaAllekirjoitettu() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoAllekirjoitettu(
            opintooikeus,
            vastuuhenkilo,
            virkailija,
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
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ALLEKIRJOITETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForHyvaksyjaTilaAllekirjoitettu() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoAllekirjoitettu(
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
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ALLEKIRJOITETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForArvioijaHyvaksyjaTilaAllekirjoitettu() {
        initTest(listOf(VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA))

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoAllekirjoitettu(
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
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ALLEKIRJOITETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
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

        val erikoistuvaLaakari = initErikoistuvaLaakari()

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)
        vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)

        vastuuhenkilo.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkilo,
                yliopisto = opintooikeus.yliopisto,
                erikoisala = opintooikeus.erikoisala,
                vastuuhenkilonTehtavat = tehtavatyypit.filter { it.nimi in vastuuhenkilonTehtavatyypit }.toMutableSet()
            )
        )
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
            anotherVastuuhenkilo.yliopistotAndErikoisalat.add(
                KayttajaYliopistoErikoisala(
                    kayttaja = anotherVastuuhenkilo,
                    yliopisto = opintooikeus.yliopisto,
                    erikoisala = opintooikeus.erikoisala,
                    vastuuhenkilonTehtavat = tehtavatyypit.filter { it.nimi !in vastuuhenkilonTehtavatyypit }
                        .toMutableSet()
                )
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
