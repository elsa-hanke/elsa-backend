package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koulutus.Opintosuoritus
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.suoritteet.Suorite
import fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta
import fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.whenever

class EtusivunEdistyminenTest {
    private val tiedot = EtusivunTestitiedot()
    private val palvelu = tiedot.luoPalvelu()
    private val oppaanPaiva = LocalDate.of(2020, 1, 1)

    @Test
    fun `tyhjan seurannan arvioiden keskiarvo puuttuu ja maarat ovat nollia`() {
        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.arviointienKeskiarvo).isNull()
        assertThat(tulos.arvioitavatKokonaisuudetVahintaanYksiArvioLkm).isZero()
        assertThat(tulos.suoritemerkinnatLkm).isZero()
        assertThat(tulos.osaalueetSuoritettuLkm).isZero()
        assertThat(tulos.koejaksoTila).isEqualTo(KoejaksoTila.EI_AKTIIVINEN)
    }

    @Test
    fun `keskiarvo lasketaan kokonaisuuksien parhaista arvioista ja tyhjat arviot ohitetaan`() {
        val kokonaisuus = ArvioitavaKokonaisuus(id = 1L, voimassaoloAlkaa = oppaanPaiva)
        val vanha = ArvioitavaKokonaisuus(id = 2L, voimassaoloAlkaa = oppaanPaiva.minusYears(2), voimassaoloLoppuu = oppaanPaiva.minusDays(1))
        val arviointi = Suoritusarviointi(arvioitavatKokonaisuudet = mutableSetOf(
            SuoritusarvioinninArvioitavaKokonaisuus(arvioitavaKokonaisuus = kokonaisuus, arviointiasteikonTaso = 2),
            SuoritusarvioinninArvioitavaKokonaisuus(arvioitavaKokonaisuus = kokonaisuus, arviointiasteikonTaso = 5),
            SuoritusarvioinninArvioitavaKokonaisuus(arvioitavaKokonaisuus = vanha, arviointiasteikonTaso = 3),
            SuoritusarvioinninArvioitavaKokonaisuus(arvioitavaKokonaisuus = kokonaisuus)
        ))
        whenever(tiedot.suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(11L)).thenReturn(listOf(arviointi))
        whenever(tiedot.arvioitavanKokonaisuudenKategoriaRepository.findAllByErikoisalaIdAndValid(2L, oppaanPaiva))
            .thenReturn(listOf(ArvioitavanKokonaisuudenKategoria(arvioitavatKokonaisuudet = mutableSetOf(kokonaisuus))))

        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.arviointienKeskiarvo).isEqualTo(4.0)
        assertThat(tulos.arvioitavatKokonaisuudetVahintaanYksiArvioLkm).isEqualTo(2)
        assertThat(tulos.arvioitavienKokonaisuuksienLkm).isEqualTo(2)
    }

    @Test
    fun `suoritemaaran ylitys rajataan mutta vanhat merkinnat kasvattavat vaatimusta kukin erikseen`() {
        val voimassa = Suorite(id = 1L, voimassaolonAlkamispaiva = oppaanPaiva, voimassaolonPaattymispaiva = oppaanPaiva, vaadittulkm = 2)
        val vanha = Suorite(id = 2L, voimassaolonAlkamispaiva = oppaanPaiva.minusYears(1), voimassaolonPaattymispaiva = oppaanPaiva.minusDays(1), vaadittulkm = 5)
        val vapaaehtoinen = Suorite(id = 3L, voimassaolonAlkamispaiva = oppaanPaiva)
        whenever(tiedot.suoritemerkintaRepository.findAllByTyoskentelyjaksoOpintooikeusId(11L)).thenReturn(
            List(3) { Suoritemerkinta(suorite = voimassa) } + List(2) { Suoritemerkinta(suorite = vanha) } + Suoritemerkinta(suorite = vapaaehtoinen)
        )
        whenever(tiedot.suoritteenKategoriaRepository.findAllByErikoisalaIdAndValid(2L, oppaanPaiva))
            .thenReturn(listOf(SuoritteenKategoria(suoritteet = mutableSetOf(voimassa, vapaaehtoinen))))

        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.suoritemerkinnatLkm).isEqualTo(5)
        assertThat(tulos.vaaditutSuoritemerkinnatLkm).isEqualTo(4)
        assertThat(tulos.osaalueetSuoritettuLkm).isEqualTo(2)
        assertThat(tulos.osaalueetVaadittuLkm).isEqualTo(2)
    }

    @Test
    fun `opintopisteet huomioidaan hyvaksynnasta riippumatta mutta kuulustelu vaatii hyvaksytyn suorituksen`() {
        whenever(tiedot.opintosuoritusRepository.findAllByOpintooikeusId(11L)).thenReturn(listOf(
            suoritus(OpintosuoritusTyyppiEnum.JOHTAMISOPINTO).apply { opintopisteet = 3.0 },
            suoritus(OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS).apply { opintopisteet = 2.0 },
            suoritus(OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU),
            suoritus(OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU).apply { hyvaksytty = true }
        ))
        whenever(tiedot.teoriakoulutusRepository.findAllByOpintooikeusId(11L)).thenReturn(mutableListOf(
            Teoriakoulutus(erikoistumiseenHyvaksyttavaTuntimaara = 4.5), Teoriakoulutus()
        ))

        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.johtamisopinnotSuoritettu).isEqualTo(3.0)
        assertThat(tulos.sateilysuojakoulutuksetSuoritettu).isEqualTo(2.0)
        assertThat(tulos.valtakunnallisetKuulustelutSuoritettuLkm).isEqualTo(1)
        assertThat(tulos.teoriakoulutuksetSuoritettu).isEqualTo(4.5)
    }

    @Test
    fun `yek teoriakoulutuksen merkinta tayttaa vaaditun tuntimaaran ja yek suoritukset tulevat muiltakin opintooikeuksilta`() {
        tiedot.opintooikeus.erikoisala!!.id = YEK_ERIKOISALA_ID
        val paiva = oppaanPaiva.minusDays(1)
        whenever(tiedot.opintosuoritusRepository.findAllByOpintooikeusIdAndTyyppi(11L, OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS))
            .thenReturn(listOf(suoritus(OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS)))
        whenever(tiedot.opintosuoritusRepository.findAllByErikoistuvaLaakariIdAndErikoisalaId(21L, YEK_ERIKOISALA_ID)).thenReturn(listOf(
            suoritus(OpintosuoritusTyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO).apply { hyvaksytty = true },
            suoritus(OpintosuoritusTyyppiEnum.YEK_PATEVYYS).apply { suorituspaiva = paiva }
        ))

        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.teoriakoulutuksetSuoritettu).isEqualTo(60.0)
        assertThat(tulos.terveyskeskuskoulutusjaksoSuoritettu).isTrue()
        assertThat(tulos.yekSuoritusPvm).isEqualTo(paiva)
    }

    @Test
    fun `hyvaksytty koejaksomerkinta ohittaa keskeneraiset vaiheet`() {
        whenever(tiedot.opintosuoritusRepository.findAllByOpintooikeusId(11L))
            .thenReturn(listOf(suoritus(OpintosuoritusTyyppiEnum.KOEJAKSO).apply { hyvaksytty = true }))

        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.koejaksoTila).isEqualTo(KoejaksoTila.HYVAKSYTTY)
        assertThat(tulos.koejaksonSuoritusmerkintaExists).isTrue()
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 1])
    fun `koulutussopimuksen aloituspaiva ratkaisee odottavan koejakson`(paivia: Int) {
        whenever(tiedot.koejaksonKoulutussopimusRepository.findByOpintooikeusId(11L))
            .thenReturn(Optional.of(KoejaksonKoulutussopimus(koejaksonAlkamispaiva = LocalDate.now(tiedot.clock).plusDays(paivia.toLong()))))

        val tulos = palvelu.getErikoistumisenSeurantaForErikoistuja("opiskelija")!!

        assertThat(tulos.koejaksoTila).isEqualTo(if (paivia <= 0) KoejaksoTila.ODOTTAA_HYVAKSYNTAA else KoejaksoTila.EI_AKTIIVINEN)
    }

    private fun suoritus(tyyppi: OpintosuoritusTyyppiEnum) = Opintosuoritus(tyyppi = OpintosuoritusTyyppi(nimi = tyyppi))
}
