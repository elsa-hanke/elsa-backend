package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.AsetusDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotTableDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksotTilastotDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.perustiedot.ErikoisalaService
import fi.elsapalvelu.elsa.service.tyoskentely.KeskeytysaikaService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksojenTaulukkopalvelu
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariTyoskentelyjaksoResource
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.yekkoulutettava.YekKoulutettavaTyoskentelyjaksoResource
import java.security.Principal
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.http.ResponseEntity

class TyoskentelyjaksonTaulukkoTest {
    enum class Koulutus(val opintooikeusId: Long) { ERIKOISTUMINEN(11), YEK(22) }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `taulukko kokoaa valitun opintooikeuden tiedot ja sailyttaa jarjestyksen ilman kaksoiskappaleita`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val ensimmainenJakso = TyoskentelyjaksoDTO(id = 2)
        val toinenJakso = TyoskentelyjaksoDTO(id = 1)
        val ensimmainenKeskeytys = KeskeytysaikaDTO(id = 8)
        val toinenKeskeytys = KeskeytysaikaDTO(id = 7)
        whenever(tiedot.jaksot.findAllByOpintooikeusId(any())).thenReturn(listOf(ensimmainenJakso, toinenJakso, TyoskentelyjaksoDTO(id = 2)))
        whenever(tiedot.keskeytysajat.findAllByTyoskentelyjaksoOpintooikeusId(any()))
            .thenReturn(listOf(ensimmainenKeskeytys, toinenKeskeytys, KeskeytysaikaDTO(id = 8)))

        val taulukko = tiedot.hae()

        assertThat(taulukko.tyoskentelyjaksot).containsExactly(ensimmainenJakso, toinenJakso)
        assertThat(taulukko.tyoskentelyjaksot.first()).isSameAs(ensimmainenJakso)
        assertThat(taulukko.keskeytykset).containsExactly(ensimmainenKeskeytys, toinenKeskeytys)
        assertThat(taulukko.keskeytykset.first()).isSameAs(ensimmainenKeskeytys)
        assertThat(taulukko.tilastot).isSameAs(tiedot.tilastot)
        tiedot.tarkistaPerustietojenHaku()
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `rekisteroity suoritus ohittaa hakemuksen ja keston tarkistuksen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.opintosuoritukset.getTerveyskoulutusjaksoSuoritusPvm(any(), any())).thenReturn(suorituspaiva)
        whenever(tiedot.hyvaksynnat.findByOpintooikeusId(any())).thenReturn(
            TerveyskeskuskoulutusjaksonHyvaksyntaDTO(tila = TerveyskeskuskoulutusjaksoTila.PALAUTETTU_KORJATTAVAKSI,
                virkailijanKorjausehdotus = "Korjaa", vastuuhenkilonKuittausaika = kuittauspaiva)
        )

        val taulukko = tiedot.hae()

        assertThat(taulukko.terveyskeskuskoulutusjaksonTila).isEqualTo(TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY)
        assertThat(taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm).isEqualTo(suorituspaiva)
        assertThat(taulukko.terveyskeskuskoulutusjaksonKorjausehdotus).isNull()
        verify(tiedot.opintosuoritukset).getTerveyskoulutusjaksoSuoritusPvm(koulutus.opintooikeusId, 33)
        verifyNoInteractions(tiedot.hyvaksynnat)
    }

    @ParameterizedTest
    @MethodSource("hyvaksyntatilat")
    fun `olemassa olevan hakemuksen tila ja kuittauspaiva valitetaan muuttamatta`(koulutus: Koulutus, tila: TerveyskeskuskoulutusjaksoTila?) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksynnat.findByOpintooikeusId(any())).thenReturn(TerveyskeskuskoulutusjaksonHyvaksyntaDTO(
            tila = tila, virkailijanKorjausehdotus = "Virkailija", vastuuhenkilonKorjausehdotus = "Vastuuhenkilö",
            vastuuhenkilonKuittausaika = kuittauspaiva
        ))

        val taulukko = tiedot.hae()

        assertThat(taulukko.terveyskeskuskoulutusjaksonTila).isEqualTo(tila)
        assertThat(taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm).isEqualTo(kuittauspaiva)
        assertThat(taulukko.terveyskeskuskoulutusjaksonKorjausehdotus).isEqualTo("Virkailija")
        verify(tiedot.hyvaksynnat).findByOpintooikeusId(koulutus.opintooikeusId)
        tiedot.tarkistaEiKestonTarkistusta()
    }

    @ParameterizedTest
    @MethodSource("korjausehdotukset")
    fun `virkailijan korjausehdotus ohittaa vastuuhenkilon myos tyhjana`(
        koulutus: Koulutus, virkailijanEhdotus: String?, vastuuhenkilonEhdotus: String?, odotettuEhdotus: String?
    ) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksynnat.findByOpintooikeusId(any())).thenReturn(TerveyskeskuskoulutusjaksonHyvaksyntaDTO(
            virkailijanKorjausehdotus = virkailijanEhdotus, vastuuhenkilonKorjausehdotus = vastuuhenkilonEhdotus
        ))

        val taulukko = tiedot.hae()

        assertThat(taulukko.terveyskeskuskoulutusjaksonKorjausehdotus).isEqualTo(odotettuEhdotus)
        assertThat(taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm).isNull()
        tiedot.tarkistaEiKestonTarkistusta()
    }

    @ParameterizedTest
    @MethodSource("kestot")
    fun `hakemuksen puuttuessa vain koulutuskohtainen kesto ratkaisee uuden tilan`(koulutus: Koulutus, suoritettu: Boolean) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksynnat.getTerveyskoulutusjaksoSuoritettu(any())).thenReturn(suoritettu)
        whenever(tiedot.hyvaksynnat.getTerveyskoulutusjaksoSuoritettuYek(any())).thenReturn(suoritettu)

        val taulukko = tiedot.hae()

        assertThat(taulukko.terveyskeskuskoulutusjaksonTila).isEqualTo(if (suoritettu) TerveyskeskuskoulutusjaksoTila.UUSI else null)
        assertThat(taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm).isNull()
        assertThat(taulukko.terveyskeskuskoulutusjaksonKorjausehdotus).isNull()
        if (koulutus == Koulutus.YEK) {
            verify(tiedot.hyvaksynnat).getTerveyskoulutusjaksoSuoritettuYek(22)
            verify(tiedot.hyvaksynnat, never()).getTerveyskoulutusjaksoSuoritettu(any())
        } else {
            verify(tiedot.hyvaksynnat).getTerveyskoulutusjaksoSuoritettu(11)
            verify(tiedot.hyvaksynnat, never()).getTerveyskoulutusjaksoSuoritettuYek(any())
        }
    }

    @Test
    fun `hammaslaaketieteen poikkeus ohittaa rekisteroidyn suorituksen ja hakemuksen`() {
        val tiedot = Testitiedot(Koulutus.ERIKOISTUMINEN)
        whenever(tiedot.erikoisalat.findOne(50)).thenReturn(Optional.of(ErikoisalaDTO(tyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE)))
        whenever(tiedot.opintosuoritukset.getTerveyskoulutusjaksoSuoritusPvm(any(), any())).thenReturn(suorituspaiva)

        val taulukko = tiedot.hae()

        assertThat(taulukko.terveyskeskuskoulutusjaksonTila).isEqualTo(TerveyskeskuskoulutusjaksoTila.HYVAKSYTTY)
        assertThat(taulukko.terveyskeskuskoulutusjaksonHyvaksymispvm).isNull()
        assertThat(taulukko.terveyskeskuskoulutusjaksonKorjausehdotus).isEqualTo("HAMMASLAAKETIEDE")
        tiedot.tarkistaPerustietojenHaku()
        verify(tiedot.erikoisalat).findOne(50)
        verifyNoInteractions(tiedot.opintosuoritukset, tiedot.hyvaksynnat)
    }

    @Test
    fun `yek ei kayta hammaslaaketieteen poikkeusta eika hae erikoisalaa`() {
        val tiedot = Testitiedot(Koulutus.YEK)
        whenever(tiedot.erikoisalat.findOne(any())).thenReturn(Optional.of(ErikoisalaDTO(tyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE)))

        assertThat(tiedot.hae().terveyskeskuskoulutusjaksonTila).isNull()

        verifyNoInteractions(tiedot.erikoisalat)
        verify(tiedot.hyvaksynnat).getTerveyskoulutusjaksoSuoritettuYek(22)
    }

    @Test
    fun `erikoistujan taulukko hylkaa yek opintooikeuden ennen tietojen hakua`() {
        val tiedot = Testitiedot(Koulutus.ERIKOISTUMINEN)
        tiedot.opintooikeus.erikoisalaId = YEK_ERIKOISALA_ID

        val virhe = assertThrows<BadRequestAlertException> { tiedot.hae() }

        assertThat(virhe.entityName).isEqualTo("tyoskentelyjakso")
        assertThat(virhe.errorKey).isEqualTo("yekopintooikeus")
        assertThat(virhe.defaultMessage).isEqualTo("Erikoistuvan työskentelyjaksoihin ei pääse YEK erikoisalan opinto-oikeudella")
        verifyNoInteractions(tiedot.jaksot, tiedot.keskeytysajat, tiedot.erikoistujat, tiedot.erikoisalat, tiedot.opintosuoritukset, tiedot.hyvaksynnat)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `puuttuva erikoisala ei esta rekisteroidyn suorituksen kayttoa`(puuttuvaTunniste: Boolean) {
        val tiedot = Testitiedot(Koulutus.ERIKOISTUMINEN)
        if (puuttuvaTunniste) tiedot.opintooikeus.erikoisalaId = null
        whenever(tiedot.opintosuoritukset.getTerveyskoulutusjaksoSuoritusPvm(any(), any())).thenReturn(suorituspaiva)

        assertThat(tiedot.hae().terveyskeskuskoulutusjaksonHyvaksymispvm).isEqualTo(suorituspaiva)

        if (puuttuvaTunniste) verifyNoInteractions(tiedot.erikoisalat) else verify(tiedot.erikoisalat).findOne(50)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `tyhjat jaksot ja keskeytykset palautetaan tyhjina joukkoina`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)

        val taulukko = tiedot.hae()

        assertThat(taulukko.tyoskentelyjaksot).isEmpty()
        assertThat(taulukko.keskeytykset).isEmpty()
        assertThat(taulukko.tilastot).isSameAs(tiedot.tilastot)
        assertThat(taulukko.terveyskeskuskoulutusjaksonTila).isNull()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `puuttuva erikoistuja hylataan perustietojen jalkeen ennen terveyskeskuskoulutuksen hakua`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.erikoistujat.findOneByKayttajaUserId(any())).thenReturn(null)

        assertThrows<IllegalArgumentException> { tiedot.hae() }

        tiedot.tarkistaPerustietojenHaku()
        verifyNoInteractions(tiedot.erikoisalat, tiedot.opintosuoritukset, tiedot.hyvaksynnat)
    }

    private class Testitiedot(private val koulutus: Koulutus) {
        val kayttajat = mock<UserService>()
        val opintooikeudet = mock<OpintooikeusService>()
        val jaksot = mock<TyoskentelyjaksoService>()
        val keskeytysajat = mock<KeskeytysaikaService>()
        val erikoistujat = mock<ErikoistuvaLaakariService>()
        val erikoisalat = mock<ErikoisalaService>()
        val opintosuoritukset = mock<OpintosuoritusService>()
        val hyvaksynnat = mock<TerveyskeskuskoulutusjaksonHyvaksyntaService>()
        val tilastot = mock<TyoskentelyjaksotTilastotDTO>()
        val opintooikeus = OpintooikeusDTO(id = 11, erikoisalaId = 50, asetus = AsetusDTO(), tila = OpintooikeudenTila.AKTIIVINEN)
        private val principal = Principal { "opiskelija" }
        private val haeTaulukko: (Principal?) -> ResponseEntity<TyoskentelyjaksotTableDTO>

        init {
            whenever(kayttajat.getAuthenticatedUser(anyOrNull())).thenReturn(UserDTO(id = "opiskelija"))
            whenever(opintooikeudet.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")).thenReturn(opintooikeus)
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)).thenReturn(22)
            whenever(erikoistujat.findOneByKayttajaUserId("opiskelija")).thenReturn(ErikoistuvaLaakariDTO(id = 33))
            whenever(erikoisalat.findOne(any())).thenReturn(Optional.empty())
            whenever(jaksot.getTilastot(any<Long>())).thenReturn(tilastot)
            val taulukkopalvelu = TyoskentelyjaksojenTaulukkopalvelu(jaksot, keskeytysajat, erikoistujat, erikoisalat, opintosuoritukset, hyvaksynnat)
            if (koulutus == Koulutus.YEK) {
                val resurssi = YekKoulutettavaTyoskentelyjaksoResource(
                    kayttajat, jaksot, keskeytysajat,
                    mock(), mock(), mock(),
                    opintooikeudet, hyvaksynnat, mock(),
                    mock(), taulukkopalvelu, mock()
                )
                haeTaulukko = resurssi::getTyoskentelyjaksoTable
            } else {
                val resurssi = ErikoistuvaLaakariTyoskentelyjaksoResource(
                    kayttajat, jaksot, keskeytysajat,
                    mock(), mock(), mock(),
                    opintooikeudet, hyvaksynnat, mock(),
                    mock(), taulukkopalvelu, mock()
                )
                haeTaulukko = resurssi::getTyoskentelyjaksoTable
            }
        }

        fun hae(): TyoskentelyjaksotTableDTO {
            val vastaus = haeTaulukko(principal)
            assertThat(vastaus.statusCode.value()).isEqualTo(200)
            return requireNotNull(vastaus.body)
        }

        fun tarkistaPerustietojenHaku() {
            inOrder(jaksot, keskeytysajat, erikoistujat) {
                verify(jaksot).findAllByOpintooikeusId(koulutus.opintooikeusId)
                verify(keskeytysajat).findAllByTyoskentelyjaksoOpintooikeusId(koulutus.opintooikeusId)
                verify(jaksot).getTilastot(koulutus.opintooikeusId)
                verify(erikoistujat).findOneByKayttajaUserId("opiskelija")
            }
        }

        fun tarkistaOpintooikeudenValinta() {
            if (koulutus == Koulutus.YEK) {
                verify(opintooikeudet).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)
                verify(opintooikeudet, never()).findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(any())
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(any())
            } else {
                verify(opintooikeudet).findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(any(), any())
            }
        }

        fun tarkistaEiKestonTarkistusta() {
            verify(hyvaksynnat, never()).getTerveyskoulutusjaksoSuoritettu(any())
            verify(hyvaksynnat, never()).getTerveyskoulutusjaksoSuoritettuYek(any())
        }
    }

    companion object {
        private val suorituspaiva = LocalDate.of(2020, 2, 29)
        private val kuittauspaiva = LocalDate.of(2020, 3, 1)

        @JvmStatic
        fun hyvaksyntatilat(): List<Arguments> = Koulutus.entries.flatMap { koulutus ->
            (TerveyskeskuskoulutusjaksoTila.entries.toList() + null).map { Arguments.of(koulutus, it) }
        }

        @JvmStatic
        fun korjausehdotukset(): List<Arguments> = Koulutus.entries.flatMap { koulutus ->
            listOf(Arguments.of(koulutus, null, "Vastuuhenkilö", "Vastuuhenkilö"), Arguments.of(koulutus, "", "Vastuuhenkilö", ""),
                Arguments.of(koulutus, " ", "Vastuuhenkilö", " "), Arguments.of(koulutus, null, null, null))
        }

        @JvmStatic
        fun kestot(): List<Arguments> = Koulutus.entries.flatMap { koulutus ->
            listOf(false, true).map { Arguments.of(koulutus, it) }
        }
    }
}
