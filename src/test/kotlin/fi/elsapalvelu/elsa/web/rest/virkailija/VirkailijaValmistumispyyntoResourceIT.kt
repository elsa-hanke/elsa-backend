package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ValmistumispyynnonTarkistusRepository
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.service.dto.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyyntoHelper
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
        initTest(Yliopisto(2))

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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content[0].id").value(valmistumispyynto.id))
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
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
    fun getValmistumispyynnotForVirkailijaTilaOdottaaAllekirjoituksia() {
        initTest()

        val valmistumispyynto =
            ValmistumispyyntoHelper.createValmistumispyyntoOdottaaAllekirjoituksia(
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
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ODOTTAA_ALLEKIRJOITUKSIA.toString()))
            .andExpect(jsonPath("$.content[0].erikoistujanNimi").value(valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.content[0].tapahtumanAjankohta").value(valmistumispyynto.muokkauspaiva.toString()))
            .andExpect(jsonPath("$.content[0].isAvoinForCurrentKayttaja").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyynnotForVirkailijaTilaAllekirjoitettu() {
        initTest()

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoAllekirjoitettu(
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
            .andExpect(jsonPath("$.content[0].tila").value(ValmistumispyynnonTila.ALLEKIRJOITETTU.toString()))
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

        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusDTO(
            yekSuoritettu = true,
            yekSuorituspaiva = yekSuorituspaiva,
            ptlSuoritettu = true,
            ptlSuorituspaiva = ptlSuorituspaiva,
            aiempiElKoulutusSuoritettu = true,
            aiempiElKoulutusSuorituspaiva = aiempiElKoulutusSuorituspaiva,
            ltTutkintoSuoritettu = true,
            ltTutkintoSuorituspaiva = ltTutkintoSuorituspaiva,
            yliopistosairaalanUlkopuolinenTyoTarkistettu = true,
            yliopistosairaalatyoTarkistettu = true,
            kokonaistyoaikaTarkistettu = true,
            teoriakoulutusTarkistettu = true,
            kommentitVirkailijoille = "test",
            keskenerainen = false
        )

        restValmistumispyyntoMockMvc.perform(
            put(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyynnonTarkistusDTO))
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

        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusDTO(
            yekSuoritettu = true,
            yekSuorituspaiva = yekSuorituspaiva,
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

        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusDTO(
            ptlSuoritettu = true,
            ptlSuorituspaiva = ptlSuorituspaiva,
            aiempiElKoulutusSuoritettu = true,
            aiempiElKoulutusSuorituspaiva = aiempiElKoulutusSuorituspaiva,
            ltTutkintoSuoritettu = true,
            ltTutkintoSuorituspaiva = ltTutkintoSuorituspaiva,
            yliopistosairaalanUlkopuolinenTyoTarkistettu = true,
            yliopistosairaalatyoTarkistettu = true,
            kokonaistyoaikaTarkistettu = true,
            teoriakoulutusTarkistettu = true,
            kommentitVirkailijoille = "test",
            keskenerainen = false
        )

        restValmistumispyyntoMockMvc.perform(
            put(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyynnonTarkistusDTO))
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

        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusDTO(
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
        ).andExpect(status().isOk)

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
        val valmistumispyynnonTarkistusDTO = ValmistumispyynnonTarkistusDTO(
            valmistumispyynto = ValmistumispyyntoDTO(virkailijanKorjausehdotus = korjausehdotus)
        )

        restValmistumispyyntoMockMvc.perform(
            put(
                "$ENDPOINT_BASE_URL$VALMISTUMISPYYNNON_TARKISTUS_ENDPOINT/{id}",
                valmistumispyynto.id
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valmistumispyynnonTarkistusDTO))
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

        val erikoistuvaLaakari = initErikoistuvaLaakari()

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        vastuuhenkilo = KayttajaHelper.createEntity(em)
        em.persist(vastuuhenkilo)

        anotherVastuuhenkilo = KayttajaHelper.createEntity(em)
        em.persist(anotherVastuuhenkilo)

        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        em.persist(virkailija)

        val yliopisto = virkailijanYliopisto ?: opintooikeus.yliopisto!!
        virkailija.yliopistot.add(yliopisto)
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
