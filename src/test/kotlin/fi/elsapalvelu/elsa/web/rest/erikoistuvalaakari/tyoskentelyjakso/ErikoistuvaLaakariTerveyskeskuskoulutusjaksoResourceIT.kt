package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.tyoskentelyjakso

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.web.rest.API_TYOSKENTELYJAKSOT
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISPAIVA
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_TYYPPI
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import kotlin.test.assertEquals

class ErikoistuvaLaakariTerveyskeskuskoulutusjaksoResourceIT : ErikoistuvaLaakariTyoskentelyjaksoResourceTestBase() {

    @Test
    fun getTerveyskeskuskoulutusjaksoTooShort() {
        initTest(kaytannonKoulutus = TERVEYSKESKUSTYO)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso")).andExpect(status().isBadRequest)
    }

    @Test
    fun getTerveyskeskuskoulutusjaksoWrongKaytannonKoulutus() {
        initTest(kaytannonKoulutus = OMAN_ERIKOISALAN_KOULUTUS)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso")).andExpect(status().isBadRequest)
    }

    @Test
    fun getTerveyskeskuskoulutusjaksoWithoutVastuuhenkilo() {
        initTest(kaytannonKoulutus = TERVEYSKESKUSTYO)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user = user, paattymispaiva = DEFAULT_ALKAMISPAIVA.plusYears(1)))
        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso")).andExpect(status().isBadRequest)
    }

    @Test
    fun getTerveyskeskuskoulutusjakso() {
        initTest(kaytannonKoulutus = TERVEYSKESKUSTYO)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user = user,
            paattymispaiva = DEFAULT_ALKAMISPAIVA.plusYears(1), kaytannonKoulutus = TERVEYSKESKUSTYO))
        val opintooikeus = opintooikeusRepository.findAll().first()

        val vastuuhenkiloUser = userRepository.saveAndFlush(KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)))
        val vastuuhenkiloKayttaja = kayttajaRepository.saveAndFlush(KayttajaHelper.createEntity(em, user = vastuuhenkiloUser))
        kayttajaYliopistoErikoisalaRepository.saveAndFlush(KayttajaYliopistoErikoisala(kayttaja = vastuuhenkiloKayttaja,
            yliopisto = opintooikeus.yliopisto, erikoisala = Erikoisala(50), vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))))

        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.erikoistuvanErikoisala").value(opintooikeus.erikoisala?.nimi))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(opintooikeus.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(opintooikeus.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistuvanSyntymaaika").value(opintooikeus.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(opintooikeus.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.laillistamispaiva").value(DEFAULT_LAILLISTAMISPAIVA.toString()))
            .andExpect(jsonPath("$.laillistamispaivanLiite").value(DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.laillistamispaivanLiitteenNimi").value(DEFAULT_LAILLISTAMISTODISTUS_NIMI))
            .andExpect(jsonPath("$.laillistamispaivanLiitteenTyyppi").value(DEFAULT_LAILLISTAMISTODISTUS_TYYPPI))
            .andExpect(jsonPath("$.asetus").value(opintooikeus.asetus?.nimi)).andExpect(jsonPath("$.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimi").value(vastuuhenkiloKayttaja.getNimi()))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimike").value(vastuuhenkiloKayttaja.nimike))
    }

    @Test
    fun createTerveyskeskuskoulutusjakso() {
        initTest(kaytannonKoulutus = TERVEYSKESKUSTYO)
        initMockFiles()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user = user,
            paattymispaiva = DEFAULT_ALKAMISPAIVA.plusYears(1), kaytannonKoulutus = TERVEYSKESKUSTYO))
        val opintooikeus = opintooikeusRepository.findAll().first()

        val vastuuhenkiloUser = userRepository.saveAndFlush(KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)))
        val vastuuhenkiloKayttaja = kayttajaRepository.saveAndFlush(KayttajaHelper.createEntity(em, user = vastuuhenkiloUser))
        kayttajaYliopistoErikoisalaRepository.saveAndFlush(KayttajaYliopistoErikoisala(kayttaja = vastuuhenkiloKayttaja, yliopisto = opintooikeus.yliopisto,
            erikoisala = Erikoisala(50), vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))))

        val laillistamispaiva = LocalDate.now()

        testMockMvc.perform(multipart("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakson-hyvaksynta")
            .file(MockMultipartFile("laillistamispaivanLiite", ASIAKIRJA_PNG_NIMI, ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes()))
            .param("laillistamispaiva", laillistamispaiva.toString()).with { it.method = "POST"; it }.with(csrf())).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").exists())

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)

        assertEquals(laillistamispaiva, erikoistuvaLaakari?.laillistamispaiva)
        assertThat(erikoistuvaLaakari?.laillistamistodistus).isNotNull
        assertEquals(ASIAKIRJA_PNG_NIMI, erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi)
        assertEquals(ASIAKIRJA_PNG_TYYPPI, erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi)
    }
}
