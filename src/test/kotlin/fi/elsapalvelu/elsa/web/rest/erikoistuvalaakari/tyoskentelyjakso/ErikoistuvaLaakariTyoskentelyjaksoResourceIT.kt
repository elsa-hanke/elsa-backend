package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.tyoskentelyjakso

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.web.rest.API_TYOSKENTELYJAKSOT
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PDF_DATA
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PDF_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_DATA
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_KAYTANNON_KOULUTUS
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_OSAAIKAPROSENTTI
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA
import fi.elsapalvelu.elsa.web.rest.helpers.KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelypaikkaHelper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import kotlin.test.assertNotNull

class ErikoistuvaLaakariTyoskentelyjaksoResourceIT : ErikoistuvaLaakariTyoskentelyjaksoResourceTestBase() {

    @Test
    fun createTyoskentelyjakso() {
        initTest()
        initMockFiles()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        testMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).file(mockMultipartFile1).file(mockMultipartFile2)
            .param("tyoskentelyjaksoJson", objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))).with(csrf())).andExpect(status().isCreated)

        val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(kirjautunutErikoistuvaLaakari)
        val defaultTyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.opintooikeus?.id).isEqualTo(kirjautunutErikoistuvaLaakari.getOpintooikeusKaytossa()?.id)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(defaultTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(defaultTyoskentelypaikka.tyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isNull()
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(defaultTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(DEFAULT_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(DEFAULT_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva).isNull()
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
        assertThat(testTyoskentelyjakso.asiakirjat).hasSize(2)

        assertTyoskentelyjaksoAsiakirja(testTyoskentelyjakso.asiakirjat.first(), ASIAKIRJA_PDF_NIMI, ASIAKIRJA_PDF_TYYPPI, ASIAKIRJA_PDF_DATA)
        assertTyoskentelyjaksoAsiakirja(testTyoskentelyjakso.asiakirjat.last(), ASIAKIRJA_PNG_NIMI, ASIAKIRJA_PNG_TYYPPI, ASIAKIRJA_PNG_DATA)
    }

    @Test
    fun createTyoskentelyjaksoWithExistingId() {
        initTest()
        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size
        tyoskentelyjakso.id = 1L
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        testMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)
        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createTyoskentelyjaksoWithExistingTyoskentelypaikkaId() {
        initTest()
        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size
        tyoskentelyjakso.tyoskentelypaikka!!.id = 1L
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        testMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)
        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createTyoskentelyjaksoWithInvalidDates() {
        initTest()
        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size
        tyoskentelyjakso.alkamispaiva = LocalDate.of(2020, 1, 25)
        tyoskentelyjakso.paattymispaiva = LocalDate.of(2020, 1, 5)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        testMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)
        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun getTyoskentelyjakso() {
        initTest()
        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso))
        persistTyoskentelyjakso(tyoskentelyjakso)

        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tyoskentelyjakso.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.osaaikaprosentti").value(DEFAULT_OSAAIKAPROSENTTI))
            .andExpect(jsonPath("$.kaytannonKoulutus").value(DEFAULT_KAYTANNON_KOULUTUS.toString()))
            .andExpect(jsonPath("$.hyvaksyttyAiempaanErikoisalaan").value(DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN))
            .andExpect(jsonPath("$.asiakirjat").value(hasSize<Any>(1))).andExpect(jsonPath("$.asiakirjat[0].id").exists())
            .andExpect(jsonPath("$.asiakirjat[0].nimi").value(ASIAKIRJA_PDF_NIMI))
            .andExpect(jsonPath("$.asiakirjat[0].lisattypvm").value(Matchers.containsString(LocalDate.now().toString())))
            .andExpect(jsonPath("$.asiakirjat[0].tyyppi").value(ASIAKIRJA_PDF_TYYPPI))
            .andExpect(jsonPath("$.asiakirjat[0].asiakirjaData.fileInputStream").doesNotExist()).andExpect(jsonPath("$.asiakirjat[0].asiakirjaData.fileSize").doesNotExist())
    }

    @Test
    fun getAnotherUserTyoskentelyjakso() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)
        persistTyoskentelyjakso(tyoskentelyjakso)
        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id)).andExpect(status().isNotFound)
    }

    @Test
    fun getKeskeytysaika() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        assertNotNull(keskeytysaika.id)

        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(keskeytysaika.id as Any)).andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString())).andExpect(jsonPath("$.poissaoloprosentti").value(DEFAULT_POISSAOLOPROSENTTI))
    }

    @Test
    fun getAnotherUserKeskeytysaika() {
        initTest(createPersistedErikoistuvaLaakari().kayttaja?.user?.id)
        persistTyoskentelyjakso(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        assertNotNull(keskeytysaika.id)

        testMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id)).andExpect(status().isNotFound)
    }

    @Test
    fun getKeskeytysaikaForm() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)

        testMockMvc.perform(get("/api/erikoistuva-laakari/poissaolo-lomake")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.poissaolonSyyt").value(hasSize<Any>(2))).andExpect(jsonPath("$.tyoskentelyjaksot").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$.tyoskentelyjaksot[0].id").value(tyoskentelyjakso.id as Any))
    }

    @Test
    fun getTyoskentelyjaksoForm() {
        initTest()
        persistTyoskentelyjakso(tyoskentelyjakso)

        testMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjakso-lomake")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kunnat").value(hasSize<Any>(478))).andExpect(jsonPath("$.erikoisalat").value(hasSize<Any>(61)))
    }

    @Test
    fun getTyoskentelyjaksoTable() {
        initTest()
        tyoskentelyjakso.kaytannonKoulutus = TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val tyoskentelyjakso2 = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjakso2.hyvaksyttyAiempaanErikoisalaan = true
        tyoskentelyjakso2.kaytannonKoulutus = OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)
        em.detach(tyoskentelyjakso2)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        testMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(hasSize<Any>(1))).andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(45.0))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(45.0)).andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(2145.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(30.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(45.0)).andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(30.0)).andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    fun getTyoskentelyjaksoTableShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari))

        val tyoskentelyjaksoForOtherOpintooikeus = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjaksoForOtherOpintooikeus)
        em.detach(tyoskentelyjaksoForOtherOpintooikeus)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjaksoForOtherOpintooikeus)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        testMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$.keskeytykset").value(hasSize<Any>(1))).andExpect(jsonPath("$.tyoskentelyjaksot[0].id").value(tyoskentelyjaksoForOtherOpintooikeus.id))
    }

    @Test
    fun getTyoskentelyjaksoTableWithTerveyskeskusMax() {
        initTest()
        tyoskentelyjakso.opintooikeus?.opintoopas?.let {
            it.terveyskeskuskoulutusjaksonVahimmaispituus = 273.75
            it.terveyskeskuskoulutusjaksonMaksimipituus = 273.75
            opintoopasRepository.saveAndFlush(it)
        }

        tyoskentelyjakso.kaytannonKoulutus = TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjakso.paattymispaiva = tyoskentelyjakso.alkamispaiva?.plusYears(1)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjakso2 = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjakso2.kaytannonKoulutus = OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)

        testMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(hasSize<Any>(0))).andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(288.75))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(288.75)).andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1901.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(288.75)).andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(367.0)).andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    fun getTyoskentelyjaksoTableWithoutTerveyskeskusMax() {
        initTest()
        tyoskentelyjakso.opintooikeus?.opintoopas?.let {
            it.terveyskeskuskoulutusjaksonMaksimipituus = null
            opintoopasRepository.saveAndFlush(it)
        }

        tyoskentelyjakso.kaytannonKoulutus = TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjakso.paattymispaiva = tyoskentelyjakso.alkamispaiva?.plusYears(1)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjakso2 = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjakso2.kaytannonKoulutus = OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)

        testMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(hasSize<Any>(0))).andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(382.0))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(382.0)).andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1808.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(367.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(382.0)).andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(367.0)).andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    private fun assertTyoskentelyjaksoAsiakirja(testAsiakirja: Asiakirja, asiakirjaNimi: String, asiakirjaTyyppi: String, asiakirjaData: ByteArray) {
        assertThat(testAsiakirja.id).isNotNull
        assertThat(testAsiakirja.nimi).isEqualTo(asiakirjaNimi)
        assertThat(testAsiakirja.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(testAsiakirja.tyyppi).isEqualTo(asiakirjaTyyppi)
        assertThat(testAsiakirja.asiakirjaData?.data).isEqualTo(asiakirjaData)
    }
}
