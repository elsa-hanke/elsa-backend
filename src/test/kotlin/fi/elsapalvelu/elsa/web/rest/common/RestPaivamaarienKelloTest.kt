package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.arviointi.SuoritusarvioinninKommenttiService
import fi.elsapalvelu.elsa.service.arviointi.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarvioinninKommenttiDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.kayttaja.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.perustiedot.ApplicationSettingService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariKouluttajavaltuutusResource
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariSuoritusarviointiResource
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RestPaivamaarienKelloTest {
    @ParameterizedTest
    @MethodSource("paivitystapaukset")
    fun `paivitysilmoitus nakyy vain nykyhetken jalkeen ja alle kolmen vuorokauden paassa`(tapaus: Paivitystapaus) {
        val asetukset = mock<ApplicationSettingService>()
        val paivitys = tapaus.aika.aikaleima().plusSeconds(tapaus.sekunteja)
        whenever(asetukset.getDatetimeSettingValue(any())).thenReturn(paivitys)
        val resurssi = JulkisetToiminnotResource(asetukset, mock(), mock(), tapaus.aika.kello())

        assertThat(resurssi.getSeuraavaPaivitys().body).isEqualTo(if (tapaus.sekunteja in 1..259199) paivitys else null)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `kommentin luonti ja paivitys kayttavat kelloa sailyttaen luontiajan`(aika: KellonTestitapaus) {
        val kayttajat = mock<UserService>()
        val kommentit = mock<SuoritusarvioinninKommenttiService>()
        val lomake = SuoritusarvioinninKommenttiDTO()
        whenever(kayttajat.getAuthenticatedUser(null)).thenReturn(UserDTO(id = "kirjoittaja"))
        whenever(kommentit.save(lomake, "kirjoittaja")).thenAnswer { lomake.apply { id = 1L } }
        val resurssi = ErikoistuvaLaakariSuoritusarvioinninKommenttiResource(kommentit, kayttajat, aika.kello())

        resurssi.createSuoritusarvioinninKommentti(1L, lomake, null)
        assertThat(lomake.luontiaika).isEqualTo(aika.aikaleima())
        assertThat(lomake.muokkausaika).isEqualTo(aika.aikaleima())
        val aiempiAika = Instant.parse("2018-01-01T00:00:00Z")
        lomake.luontiaika = aiempiAika
        resurssi.updateSuoritusarvioinninKommentti(lomake, null)
        assertThat(lomake.muokkausaika).isEqualTo(aika.aikaleima())
        assertThat(lomake.luontiaika).isEqualTo(aiempiAika)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `REST valtuutus alkaa kellon paivana ja paattyy kuuden kuukauden kuluttua`(aika: KellonTestitapaus) {
        val kayttajat = mock<UserService>()
        val valtuutukset = mock<KouluttajavaltuutusService>()
        whenever(kayttajat.getAuthenticatedUser(null)).thenReturn(UserDTO(id = "erikoistuja"))
        whenever(valtuutukset.save(any(), any(), any())).thenAnswer { it.arguments[1] as KouluttajavaltuutusDTO }
        val resurssi = ErikoistuvaLaakariKouluttajavaltuutusResource(kayttajat, valtuutukset, aika.kello())

        val tulos = resurssi.createKouluttajavaltuutus(KayttajaDTO(id = 2L, userId = "kouluttaja"), null).body!!

        assertThat(tulos.alkamispaiva).isEqualTo(aika.paiva)
        assertThat(tulos.paattymispaiva).isEqualTo(aika.paiva.plusMonths(6))
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `arviointipyynnon paiva tulee kellosta`(aika: KellonTestitapaus) {
        val kayttajat = mock<UserService>()
        val opintooikeudet = mock<OpintooikeusService>()
        val erikoistujat = mock<ErikoistuvaLaakariService>()
        val jaksot = mock<TyoskentelyjaksoService>()
        val arviot = mock<SuoritusarviointiService>()
        val lomake = SuoritusarviointiDTO(tyoskentelyjaksoId = 2L, tapahtumanAjankohta = aika.paiva, arvioinninAntaja = KayttajaDTO(id = 3L))
        whenever(kayttajat.getAuthenticatedUser(null)).thenReturn(UserDTO(id = "erikoistuja"))
        whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("erikoistuja")).thenReturn(11L)
        whenever(erikoistujat.findOneByKayttajaUserId("erikoistuja")).thenReturn(ErikoistuvaLaakariDTO())
        whenever(jaksot.findOne(2L, 11L)).thenReturn(TyoskentelyjaksoDTO(alkamispaiva = aika.paiva.minusDays(1)))
        whenever(arviot.save(lomake)).thenAnswer { lomake.apply { id = 1L } }
        val resurssi = ErikoistuvaLaakariSuoritusarviointiResource(arviot, mock(), kayttajat, jaksot, mock(), mock(), erikoistujat,
            mock(), mock(), mock(), opintooikeudet, mock(), mock(), mock(), aika.kello())

        resurssi.createSuoritusarviointi(lomake, null)

        assertThat(lomake.pyynnonAika).isEqualTo(aika.paiva)
    }

    data class Paivitystapaus(val aika: KellonTestitapaus, val sekunteja: Long)
    companion object {
        @JvmStatic
        fun paivitystapaukset(): List<Paivitystapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(-1L, 0L, 1L, 259199L, 259200L).map { Paivitystapaus(aika, it) }
        }
    }
}
