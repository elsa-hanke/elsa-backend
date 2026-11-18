package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaFormDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.PoissaolonSyyDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.perustiedot.ErikoisalaService
import fi.elsapalvelu.elsa.service.perustiedot.KuntaService
import fi.elsapalvelu.elsa.service.PoissaolonSyyService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksojenLomakepalvelu
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariTyoskentelyjaksoResource
import fi.elsapalvelu.elsa.web.rest.yekkoulutettava.YekKoulutettavaTyoskentelyjaksoResource
import java.security.Principal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.*
import org.springframework.http.ResponseEntity

class TyoskentelyjaksojenLomakkeetTest {
    enum class Koulutus(val opintooikeusId: Long) { ERIKOISTUMINEN(11), YEK(22) }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `jaksolomake sailyttaa kuntien ja erikoisalojen jarjestyksen ja poistaa kaksoiskappaleet`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val ensimmainenKunta = KuntaDTO(id = "002")
        val toinenKunta = KuntaDTO(id = "001")
        val ensimmainenErikoisala = ErikoisalaDTO(id = 2)
        val toinenErikoisala = ErikoisalaDTO(id = 1)
        whenever(tiedot.kunnat.findAll()).thenReturn(listOf(ensimmainenKunta, toinenKunta, KuntaDTO(id = "002")))
        whenever(tiedot.erikoisalat.findAll()).thenReturn(listOf(ensimmainenErikoisala, toinenErikoisala, ErikoisalaDTO(id = 2)))

        val lomake = tiedot.haeJaksolomake()

        assertThat(lomake.kunnat).containsExactly(ensimmainenKunta, toinenKunta)
        assertThat(lomake.kunnat.first()).isSameAs(ensimmainenKunta)
        assertThat(lomake.erikoisalat).containsExactly(ensimmainenErikoisala, toinenErikoisala)
        assertThat(lomake.erikoisalat.first()).isSameAs(ensimmainenErikoisala)
        inOrder(tiedot.kunnat, tiedot.erikoisalat, tiedot.asiakirjat) {
            verify(tiedot.kunnat).findAll()
            verify(tiedot.erikoisalat).findAll()
            verify(tiedot.asiakirjat).findAllByOpintooikeusId(koulutus.opintooikeusId)
        }
        verifyNoMoreInteractions(tiedot.erikoisalat)
        verifyNoInteractions(tiedot.poissaolonSyyt, tiedot.jaksot)
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `varatut nimet deduplikoidaan muuttamatta kirjainkokoa valilyonteja tai tyhjaa nimea`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val nimet = listOf("Todistus.pdf", "todistus.pdf", " Todistus.pdf ", "", "Todistus.pdf")
        whenever(tiedot.asiakirjat.findAllByOpintooikeusId(any())).thenReturn(
            nimet.mapIndexed { indeksi, nimi -> AsiakirjaDTO(id = indeksi.toLong(), nimi = nimi) }
        )

        assertThat(tiedot.haeJaksolomake().reservedAsiakirjaNimet)
            .containsExactly("Todistus.pdf", "todistus.pdf", " Todistus.pdf ", "")
        verify(tiedot.asiakirjat).findAllByOpintooikeusId(koulutus.opintooikeusId)
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `keskeytyslomake hakee vain valitun opintooikeuden syyt ja jaksot`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val ensimmainenSyy = PoissaolonSyyDTO(id = 2)
        val toinenSyy = PoissaolonSyyDTO(id = 1)
        val ensimmainenJakso = TyoskentelyjaksoDTO(id = 4)
        val toinenJakso = TyoskentelyjaksoDTO(id = 3)
        whenever(tiedot.poissaolonSyyt.findAllByOpintooikeusId(any())).thenReturn(listOf(ensimmainenSyy, toinenSyy, PoissaolonSyyDTO(id = 2)))
        whenever(tiedot.jaksot.findAllByOpintooikeusId(any())).thenReturn(listOf(ensimmainenJakso, toinenJakso, TyoskentelyjaksoDTO(id = 4)))

        val lomake = tiedot.haeKeskeytyslomake()

        assertThat(lomake.poissaolonSyyt).containsExactly(ensimmainenSyy, toinenSyy)
        assertThat(lomake.poissaolonSyyt.first()).isSameAs(ensimmainenSyy)
        assertThat(lomake.tyoskentelyjaksot).containsExactly(ensimmainenJakso, toinenJakso)
        assertThat(lomake.tyoskentelyjaksot.first()).isSameAs(ensimmainenJakso)
        inOrder(tiedot.poissaolonSyyt, tiedot.jaksot) {
            verify(tiedot.poissaolonSyyt).findAllByOpintooikeusId(koulutus.opintooikeusId)
            verify(tiedot.jaksot).findAllByOpintooikeusId(koulutus.opintooikeusId)
        }
        verifyNoInteractions(tiedot.kunnat, tiedot.erikoisalat, tiedot.asiakirjat)
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `tyhjat lahdetiedot palautetaan tyhjina joukkoina molemmissa lomakkeissa`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)

        val jaksolomake = tiedot.haeJaksolomake()
        val keskeytyslomake = tiedot.haeKeskeytyslomake()

        assertThat(jaksolomake.kunnat).isEmpty()
        assertThat(jaksolomake.erikoisalat).isEmpty()
        assertThat(jaksolomake.reservedAsiakirjaNimet).isEmpty()
        assertThat(keskeytyslomake.poissaolonSyyt).isEmpty()
        assertThat(keskeytyslomake.tyoskentelyjaksot).isEmpty()
        tiedot.tarkistaOpintooikeudenValinta(2)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `asiakirjan puuttuva nimi hylataan kuntien ja erikoisalojen haun jalkeen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.asiakirjat.findAllByOpintooikeusId(any())).thenReturn(listOf(AsiakirjaDTO(nimi = "Todistus.pdf"), AsiakirjaDTO(nimi = null)))

        assertThrows<IllegalArgumentException> { tiedot.haeJaksolomake() }

        inOrder(tiedot.kunnat, tiedot.erikoisalat, tiedot.asiakirjat) {
            verify(tiedot.kunnat).findAll()
            verify(tiedot.erikoisalat).findAll()
            verify(tiedot.asiakirjat).findAllByOpintooikeusId(koulutus.opintooikeusId)
        }
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `kuntahaun virhe keskeyttaa muiden jaksolomaketietojen haun`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virhe = IllegalStateException("Kuntahaku epäonnistui")
        whenever(tiedot.kunnat.findAll()).thenThrow(virhe)

        assertThat(assertThrows<IllegalStateException> { tiedot.haeJaksolomake() }).isSameAs(virhe)
        verifyNoInteractions(tiedot.erikoisalat, tiedot.asiakirjat, tiedot.poissaolonSyyt, tiedot.jaksot)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `poissaolon syiden hakuvirhe keskeyttaa jaksojen haun`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virhe = IllegalStateException("Poissaolon syiden haku epäonnistui")
        whenever(tiedot.poissaolonSyyt.findAllByOpintooikeusId(any())).thenThrow(virhe)

        assertThat(assertThrows<IllegalStateException> { tiedot.haeKeskeytyslomake() }).isSameAs(virhe)
        verifyNoInteractions(tiedot.jaksot, tiedot.kunnat, tiedot.erikoisalat, tiedot.asiakirjat)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `opintooikeuden hakuvirhe estaa molempien lomakkeiden tietojen haun`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virhe = IllegalStateException("Opinto-oikeutta ei löytynyt")
        whenever(tiedot.opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(any())).thenThrow(virhe)
        whenever(tiedot.opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(any(), any())).thenThrow(virhe)

        assertThat(assertThrows<IllegalStateException> { tiedot.haeJaksolomake() }).isSameAs(virhe)
        assertThat(assertThrows<IllegalStateException> { tiedot.haeKeskeytyslomake() }).isSameAs(virhe)
        verifyNoInteractions(tiedot.kunnat, tiedot.erikoisalat, tiedot.asiakirjat, tiedot.poissaolonSyyt, tiedot.jaksot)
    }

    private class Testitiedot(private val koulutus: Koulutus) {
        val kayttajat = mock<UserService>()
        val opintooikeudet = mock<OpintooikeusService>()
        val kunnat = mock<KuntaService>()
        val erikoisalat = mock<ErikoisalaService>()
        val asiakirjat = mock<AsiakirjaService>()
        val poissaolonSyyt = mock<PoissaolonSyyService>()
        val jaksot = mock<TyoskentelyjaksoService>()
        private val principal = Principal { "opiskelija" }
        private val jaksolomake: (Principal?) -> ResponseEntity<TyoskentelyjaksoFormDTO>
        private val keskeytyslomake: (Principal?) -> ResponseEntity<KeskeytysaikaFormDTO>

        init {
            whenever(kayttajat.getAuthenticatedUser(anyOrNull())).thenReturn(UserDTO(id = "opiskelija"))
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")).thenReturn(11)
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)).thenReturn(22)
            val lomakepalvelu = TyoskentelyjaksojenLomakepalvelu(kunnat, erikoisalat, asiakirjat, poissaolonSyyt, jaksot)
            if (koulutus == Koulutus.YEK) {
                val resurssi = YekKoulutettavaTyoskentelyjaksoResource(
                    kayttajat, jaksot, mock(),
                    mock(), mock(), mock(),
                    opintooikeudet, mock(), mock(),
                    mock(), mock(), lomakepalvelu
                )
                jaksolomake = resurssi::getTyoskentelyjaksoForm
                keskeytyslomake = resurssi::getKeskeytysaikaForm
            } else {
                val resurssi = ErikoistuvaLaakariTyoskentelyjaksoResource(
                    kayttajat, jaksot, mock(),
                    mock(), mock(), mock(),
                    opintooikeudet, mock(), mock(),
                    mock(), mock(), lomakepalvelu
                )
                jaksolomake = resurssi::getTyoskentelyjaksoForm
                keskeytyslomake = resurssi::getKeskeytysaikaForm
            }
        }

        fun haeJaksolomake(): TyoskentelyjaksoFormDTO {
            val vastaus = jaksolomake(principal)
            assertThat(vastaus.statusCode.value()).isEqualTo(200)
            return requireNotNull(vastaus.body)
        }

        fun haeKeskeytyslomake(): KeskeytysaikaFormDTO {
            val vastaus = keskeytyslomake(principal)
            assertThat(vastaus.statusCode.value()).isEqualTo(200)
            return requireNotNull(vastaus.body)
        }

        fun tarkistaOpintooikeudenValinta(kertoja: Int = 1) {
            if (koulutus == Koulutus.YEK) {
                verify(opintooikeudet, times(kertoja)).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(any())
            } else {
                verify(opintooikeudet, times(kertoja)).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(any(), any())
            }
        }
    }
}
