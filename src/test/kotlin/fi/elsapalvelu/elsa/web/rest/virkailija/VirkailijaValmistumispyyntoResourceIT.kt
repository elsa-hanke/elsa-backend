package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ValmistumispyynnonTarkistusRepository
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import jakarta.persistence.EntityManager
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val ENDPOINT_BASE_URL = "/api/virkailija"
private const val VALMISTUMISPYYNNOT_ENDPOINT =
    "/valmistumispyynnot?page=0&size=20&sort=muokkauspaiva,desc"
private const val VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT = "/valmistumispyynnon-tarkistus"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VirkailijaValmistumispyyntoResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    @Autowired
    private lateinit var valmistumispyynnonTarkistusRepository: ValmistumispyynnonTarkistusRepository

    @Autowired
    private lateinit var restValmistumispyyntoMockMvc: MockMvc

    private lateinit var opintooikeus: Opintooikeus

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var vastuuhenkilo: Kayttaja

    private lateinit var virkailija: Kayttaja

    private lateinit var anotherVastuuhenkilo: Kayttaja

    @Test
    @Transactional
    fun getValmistumispyynnotForVirkailijaTilaOdottaaTarkastustaAvoin() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=true"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(true))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotEriYliopisto() {
        initTest(Yliopisto(nimi = YliopistoEnum.TURUN_YLIOPISTO))

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaArviointia(opintooikeus)
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=true"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForVirkailijaTilaOdottaaTarkastusta() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus, vastuuhenkilo
            )
        em.persist(valmistumispyynto)


        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForVirkailijaTilaPalautettuByVirkailija() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoVirkailijanTarkastusPalautettu(
                opintooikeus,
                virkailija
            )
        em.persist(valmistumispyynto)


        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=false",
            )
        )
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForVirkailijaTilaPalautettuByOsaamisenArvioija() {
        initTest()

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOsaamisenArviointiPalautettu(
                opintooikeus,
                vastuuhenkilo,
                palautusaika
            )
        em.persist(valmistumispyynto)


        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForVirkailijaTilaOdottaaHyvaksyntaa() {
        initTest()

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            opintooikeus,
            vastuuhenkilo,
            virkailija
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=false"
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
    fun getValmistumispyynnotForVirkailijaTilaPalautettuByHyvaksyja() {
        initTest()

        val palautusaika = LocalDate.now().minusDays(1)
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksyntaPalautettu(
            opintooikeus,
            vastuuhenkilo,
            palautusaika
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=false",
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
    fun getValmistumispyynnotForVirkailijaTilaHyvaksytty() {
        initTest()

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoHyvaksytty(
            opintooikeus,
            vastuuhenkilo,
            virkailija,
            anotherVastuuhenkilo
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNOT_ENDPOINT&avoin=false",
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.HYVAKSYTTY.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoForVirkailijaTilaOdottaaTarkistusta() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

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
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").isEmpty)
            .andExpect(jsonPath("$.valmistumispyynto.tila").value(ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.valmistumispyynto.muokkauspaiva").value(valmistumispyynto.erikoistujanKuittausaika.toString()))
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
    fun tryToGetValmistumispyyntoFromDifferentYliopisto() {
        initTest()

        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)
        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                erikoistuvaLaakari.getOpintooikeusKaytossa()!!,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/${valmistumispyynto.id}"
            )
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun ackValmistumispyynnonTarkistus() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        val databaseSizeBeforeUpdate = valmistumispyynnonTarkistusRepository.findAll().size

        val yekSuorituspaiva = LocalDate.ofEpochDay(15)
        val ptlSuorituspaiva = LocalDate.ofEpochDay(16)
        val aiempiElKoulutusSuorituspaiva = LocalDate.ofEpochDay(17)
        val ltTutkintoSuorituspaiva = LocalDate.ofEpochDay(18)

        restValmistumispyyntoMockMvc.perform(
            multipart(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .param("yekSuoritettu", "true")
                .param("yekSuorituspaiva", yekSuorituspaiva.toString())
                .param("ptlSuoritettu", "true")
                .param("ptlSuorituspaiva", ptlSuorituspaiva.toString())
                .param("aiempiElKoulutusSuoritettu", "true")
                .param("aiempiElKoulutusSuorituspaiva", aiempiElKoulutusSuorituspaiva.toString())
                .param("ltTutkintoSuoritettu", "true")
                .param("ltTutkintoSuorituspaiva", ltTutkintoSuorituspaiva.toString())
                .param("yliopistosairaalanUlkopuolinenTyoTarkistettu", "true")
                .param("yliopistosairaalatyoTarkistettu", "true")
                .param("kokonaistyoaikaTarkistettu", "true")
                .param("teoriakoulutusTarkistettu", "true")
                .param("kommentitVirkailijoille", "test")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnonTarkistuksetList = valmistumispyynnonTarkistusRepository.findAll()
        assertThat(valmistumispyynnonTarkistuksetList).hasSize(databaseSizeBeforeUpdate + 1)
        val updatedValmistumispyynto =
            valmistumispyynnonTarkistuksetList[valmistumispyynnonTarkistuksetList.size - 1]
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailija).isEqualTo(virkailija)
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanKuittausaika).isEqualTo(
            LocalDate.now()
        )
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanKorjausehdotus).isNull()
        assertThat(updatedValmistumispyynto.yekSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.yekSuorituspaiva).isEqualTo(yekSuorituspaiva)
        assertThat(updatedValmistumispyynto.ptlSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.ptlSuorituspaiva).isEqualTo(ptlSuorituspaiva)
        assertThat(updatedValmistumispyynto.aiempiElKoulutusSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.aiempiElKoulutusSuorituspaiva).isEqualTo(
            aiempiElKoulutusSuorituspaiva
        )
        assertThat(updatedValmistumispyynto.ltTutkintoSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.ltTutkintoSuorituspaiva).isEqualTo(
            ltTutkintoSuorituspaiva
        )
        assertThat(updatedValmistumispyynto.yliopistosairaalanUlkopuolinenTyoTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.yliopistosairaalatyoTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.kokonaistyoaikaTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.teoriakoulutusTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.kommentitVirkailijoille).isEqualTo("test")
    }

    @Test
    @Transactional
    fun ackValmistumispyynnonTarkistusKeskenerainen() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        val databaseSizeBeforeUpdate = valmistumispyynnonTarkistusRepository.findAll().size

        val yekSuorituspaiva = LocalDate.ofEpochDay(15)

        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusUpdateDTO(
            yekSuoritettu = true,
            yekSuorituspaiva = yekSuorituspaiva,
            keskenerainen = true
        )

        restValmistumispyyntoMockMvc.perform(
            multipart(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .param("yekSuoritettu", "true")
                .param("yekSuorituspaiva", yekSuorituspaiva.toString())
                .param("keskenerainen", "true")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnonTarkistuksetList = valmistumispyynnonTarkistusRepository.findAll()
        assertThat(valmistumispyynnonTarkistuksetList).hasSize(databaseSizeBeforeUpdate + 1)
        val updatedValmistumispyynto =
            valmistumispyynnonTarkistuksetList[valmistumispyynnonTarkistuksetList.size - 1]
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailija).isNull()
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanKuittausaika).isNull()
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanKorjausehdotus).isNull()
        assertThat(updatedValmistumispyynto.yekSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.yekSuorituspaiva).isEqualTo(yekSuorituspaiva)
        assertThat(updatedValmistumispyynto.ptlSuoritettu).isFalse
        assertThat(updatedValmistumispyynto.ptlSuorituspaiva).isNull()
        assertThat(updatedValmistumispyynto.aiempiElKoulutusSuoritettu).isFalse
        assertThat(updatedValmistumispyynto.aiempiElKoulutusSuorituspaiva).isNull()
        assertThat(updatedValmistumispyynto.ltTutkintoSuoritettu).isFalse
        assertThat(updatedValmistumispyynto.ltTutkintoSuorituspaiva).isNull()
        assertThat(updatedValmistumispyynto.yliopistosairaalanUlkopuolinenTyoTarkistettu).isFalse
        assertThat(updatedValmistumispyynto.yliopistosairaalatyoTarkistettu).isFalse
        assertThat(updatedValmistumispyynto.kokonaistyoaikaTarkistettu).isFalse
        assertThat(updatedValmistumispyynto.teoriakoulutusTarkistettu).isFalse
        assertThat(updatedValmistumispyynto.kommentitVirkailijoille).isNull()
    }

    @Test
    @Transactional
    fun ackExistingValmistumispyynnonTarkistus() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        val yekSuorituspaiva = LocalDate.ofEpochDay(15)
        val ptlSuorituspaiva = LocalDate.ofEpochDay(16)
        val aiempiElKoulutusSuorituspaiva = LocalDate.ofEpochDay(17)
        val ltTutkintoSuorituspaiva = LocalDate.ofEpochDay(18)

        val valmistumispyynnonTarkistus = ValmistumispyynnonTarkistus(
            valmistumispyynto = valmistumispyynto,
            yekSuoritettu = true,
            yekSuorituspaiva = yekSuorituspaiva
        )
        em.persist(valmistumispyynnonTarkistus)

        val databaseSizeBeforeUpdate = valmistumispyynnonTarkistusRepository.findAll().size

        restValmistumispyyntoMockMvc.perform(
            multipart(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .param("ptlSuoritettu", "true")
                .param("ptlSuorituspaiva", ptlSuorituspaiva.toString())
                .param("aiempiElKoulutusSuoritettu", "true")
                .param("aiempiElKoulutusSuorituspaiva", aiempiElKoulutusSuorituspaiva.toString())
                .param("ltTutkintoSuoritettu", "true")
                .param("ltTutkintoSuorituspaiva", ltTutkintoSuorituspaiva.toString())
                .param("yliopistosairaalanUlkopuolinenTyoTarkistettu", "true")
                .param("yliopistosairaalatyoTarkistettu", "true")
                .param("kokonaistyoaikaTarkistettu", "true")
                .param("teoriakoulutusTarkistettu", "true")
                .param("kommentitVirkailijoille", "test")
                .param("keskenerainen", "false")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnonTarkistuksetList = valmistumispyynnonTarkistusRepository.findAll()
        assertThat(valmistumispyynnonTarkistuksetList).hasSize(databaseSizeBeforeUpdate)
        val updatedValmistumispyynto =
            valmistumispyynnonTarkistuksetList[valmistumispyynnonTarkistuksetList.size - 1]
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailija).isEqualTo(virkailija)
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanKuittausaika).isEqualTo(
            LocalDate.now()
        )
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanPalautusaika).isNull()
        assertThat(updatedValmistumispyynto.valmistumispyynto?.virkailijanKorjausehdotus).isNull()
        assertThat(updatedValmistumispyynto.yekSuoritettu).isFalse
        assertThat(updatedValmistumispyynto.yekSuorituspaiva).isNull()
        assertThat(updatedValmistumispyynto.ptlSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.ptlSuorituspaiva).isEqualTo(ptlSuorituspaiva)
        assertThat(updatedValmistumispyynto.aiempiElKoulutusSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.aiempiElKoulutusSuorituspaiva).isEqualTo(
            aiempiElKoulutusSuorituspaiva
        )
        assertThat(updatedValmistumispyynto.ltTutkintoSuoritettu).isTrue
        assertThat(updatedValmistumispyynto.ltTutkintoSuorituspaiva).isEqualTo(
            ltTutkintoSuorituspaiva
        )
        assertThat(updatedValmistumispyynto.yliopistosairaalanUlkopuolinenTyoTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.yliopistosairaalatyoTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.kokonaistyoaikaTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.teoriakoulutusTarkistettu).isTrue
        assertThat(updatedValmistumispyynto.kommentitVirkailijoille).isEqualTo("test")
    }

    @Test
    @Transactional
    fun tryToAckValmistumispyyntoFromDifferentYliopisto() {
        initTest()

        val anotherYliopisto = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(anotherYliopisto)

        val erikoistuvaLaakari = initErikoistuvaLaakari(anotherYliopisto, opintooikeus.erikoisala)

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                erikoistuvaLaakari.getOpintooikeusKaytossa()!!,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        val databaseSizeBeforeUpdate = valmistumispyynnonTarkistusRepository.findAll().size

        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusUpdateDTO(
            yekSuoritettu = true,
            yekSuorituspaiva = LocalDate.ofEpochDay(15),
            keskenerainen = true
        )

        restValmistumispyyntoMockMvc.perform(
            put(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyynnonTarkistusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tarkistusList = valmistumispyynnonTarkistusRepository.findAll()
        assertThat(tarkistusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun declineValmistumispyynto() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaVirkailijanTarkastusta(
                opintooikeus,
                vastuuhenkilo
            )
        em.persist(valmistumispyynto)

        val databaseSizeBeforeUpdate = valmistumispyynnonTarkistusRepository.findAll().size

        val korjausehdotus = "korjausehdotus"

        restValmistumispyyntoMockMvc.perform(
            multipart(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .param("korjausehdotus", korjausehdotus)
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val tarkistuksetList = valmistumispyynnonTarkistusRepository.findAll()
        assertThat(tarkistuksetList).hasSize(databaseSizeBeforeUpdate + 1)
        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        val updatedValmistumispyynto = valmistumispyynnotList[valmistumispyynnotList.size - 1]
        assertThat(updatedValmistumispyynto.virkailija).isEqualTo(virkailija)
        assertThat(updatedValmistumispyynto.virkailijanKuittausaika).isNull()
        assertThat(updatedValmistumispyynto.virkailijanPalautusaika).isEqualTo(LocalDate.now())
        assertThat(updatedValmistumispyynto.virkailijanKorjausehdotus).isEqualTo(korjausehdotus)
    }

    fun initTest(virkailijanYliopisto: Yliopisto? = null) {
        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(virkailijaUser)
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(OPINTOHALLINNON_VIRKAILIJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(virkailijaUser.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        erikoistuvaLaakari = initErikoistuvaLaakari()

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)

        val arvioijaUser =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(name = VASTUUHENKILO))
        em.persist(arvioijaUser)
        vastuuhenkilo = KayttajaHelper.createEntity(em, arvioijaUser)
        vastuuhenkilo.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkilo,
                yliopisto = opintooikeus.yliopisto,
                erikoisala = opintooikeus.erikoisala,
                vastuuhenkilonTehtavat = tehtavatyypit.filter { it.nimi == VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI }
                    .toMutableSet()
            )
        )
        em.persist(vastuuhenkilo)

        val hyvaksyjaUser =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(name = VASTUUHENKILO))
        em.persist(hyvaksyjaUser)
        anotherVastuuhenkilo = KayttajaHelper.createEntity(em, hyvaksyjaUser)
        anotherVastuuhenkilo.yliopistotAndErikoisalat.add(
            KayttajaYliopistoErikoisala(
                kayttaja = anotherVastuuhenkilo,
                yliopisto = opintooikeus.yliopisto,
                erikoisala = opintooikeus.erikoisala,
                vastuuhenkilonTehtavat = tehtavatyypit.filter { it.nimi == VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA }
                    .toMutableSet()
            )
        )
        em.persist(anotherVastuuhenkilo)

        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        virkailijanYliopisto?.let {
            em.persist(virkailijanYliopisto)
        }
        val yliopisto = virkailijanYliopisto ?: opintooikeus.yliopisto!!
        virkailija.yliopistot.add(yliopisto)
        em.persist(virkailija)
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
