package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyajanPaallekkaisyystarkistus
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksonPituusCounterService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*

class TyoskentelyajanPaallekkaisyysRegressionTest {
    private val alku = LocalDate.of(2020, 12, 30)
    private val loppu = LocalDate.of(2021, 1, 2)
    private val jaksot = mock<TyoskentelyjaksoRepository>()
    private val keskeytysajat = mock<KeskeytysaikaRepository>()
    private val pituuslaskuri = mock<TyoskentelyjaksonPituusCounterService>()
    private val kello = Clock.fixed(loppu.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val validaattori = OverlappingTyoskentelyjaksoValidationServiceImpl(jaksot, keskeytysajat, TyoskentelyajanPaallekkaisyystarkistus(pituuslaskuri), kello)

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `paattymispaiva kuuluu tarkistukseen mutta seuraava paiva ei`(siirtyma: Int) {
        val nykyinen = jakso(1, 100).apply {
            alkamispaiva = loppu.plusDays(siirtyma.toLong())
            paattymispaiva = loppu.plusDays(1)
        }
        lataa(nykyinen)

        assertThat(tarkistaUusi(1)).isEqualTo(siirtyma == 1)
        verifyNoInteractions(pituuslaskuri)
    }

    @ParameterizedTest
    @ValueSource(ints = [75, 76])
    fun `poissaolo vahennetaan jakson osaaikaisuudella painotettuna`(uusiOsaaikaprosentti: Int) {
        val nykyinen = jakso(1, 50)
        lisaaPoissaolo(nykyinen, 50, PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN)
        lataa(nykyinen)

        assertThat(tarkistaUusi(uusiOsaaikaprosentti)).isEqualTo(uusiOsaaikaprosentti == 75)
        verifyNoInteractions(pituuslaskuri)
    }

    @Test
    fun `tasan sadan prosentin paiva ei kuluta poissaolon hyvaksilukua`() {
        val nykyinen = jakso(1, 50)
        lisaaPoissaolo(nykyinen, 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)
        lataa(nykyinen)

        assertThat(tarkistaUusi(50)).isTrue()
        verifyNoInteractions(pituuslaskuri)
    }

    @Test
    fun `hyvaksilukusaldo haetaan kerran ensimmaista sita tarvitsevaa paivaa edeltavaan paivaan`() {
        val ensimmainen = jakso(1, 50)
        lisaaPoissaolo(ensimmainen, 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)
        val toinen = jakso(2, 50).apply { alkamispaiva = alku.plusDays(1) }
        lataa(ensimmainen, toinen)
        whenever(pituuslaskuri.calculateHyvaksiluettavatDaysLeft(any(), any())).thenReturn(
            HyvaksiluettavatCounterData(hyvaksiluettavatPerYearMap = mutableMapOf(2020 to 0.0, 2021 to 0.0))
        )

        assertThat(tarkistaUusi(25)).isTrue()

        val tarkistusjaksot = argumentCaptor<List<Tyoskentelyjakso>>()
        verify(pituuslaskuri).calculateHyvaksiluettavatDaysLeft(tarkistusjaksot.capture(), eq(alku))
        verifyNoMoreInteractions(pituuslaskuri)
        val tarkistusjakso = tarkistusjaksot.firstValue.first()
        assertThat(tarkistusjakso).isNotSameAs(ensimmainen)
        assertThat(tarkistusjakso.keskeytykset.single()).isNotSameAs(ensimmainen.keskeytykset.single())
        assertThat(tarkistusjakso.keskeytykset.single().tyoskentelyjakso).isSameAs(tarkistusjakso)
    }

    @Test
    fun `ylitys lopettaa tarkistuksen ennen myohempien paivien hyvaksilukua`() {
        val ensimmainen = jakso(1, 100)
        val toinen = jakso(2, 100).apply { alkamispaiva = alku.plusDays(1) }
        lisaaPoissaolo(toinen, 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)
        lataa(ensimmainen, toinen)

        assertThat(tarkistaUusi(1)).isFalse()
        verifyNoInteractions(pituuslaskuri)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `tapahtumia sisaltavan jakson paivitys muuttaa tarkistuksessa vain paattymispaivaa`(tapahtumia: Boolean) {
        val nykyinen = jakso(1, 100)
        if (tapahtumia) lisaaPoissaolo(nykyinen, 50, PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN)
        lataa(nykyinen, jakso(2, 60))
        val ehdotus = TyoskentelyjaksoDTO(id = 1, alkamispaiva = alku, paattymispaiva = loppu, osaaikaprosentti = 40)

        assertThat(validaattori.validateTyoskentelyjakso(11, ehdotus)).isEqualTo(!tapahtumia)
        assertThat(nykyinen.osaaikaprosentti).isEqualTo(100)
        assertThat(nykyinen.alkamispaiva).isEqualTo(alku)
    }

    @Test
    fun `paivat ilman aiempia jaksoja eivat kaynnista poissaololaskentaa`() {
        lataa(jakso(1, 100).apply { paattymispaiva = alku.minusDays(1) })

        assertThat(tarkistaUusi(100)).isTrue()
        verifyNoInteractions(pituuslaskuri)
    }

    @Test
    fun `tuntemattoman keskeytysajan poisto hylataan hakematta jaksoja`() {
        assertThat(validaattori.validateKeskeytysaikaDelete(11, 99)).isFalse()
        verifyNoInteractions(jaksot, pituuslaskuri)
    }

    @Test
    fun `hyvaksilukusaldo ei jaa elamaan tarkistuskutsujen valille`() {
        val nykyinen = jakso(1, 100)
        lisaaPoissaolo(nykyinen, 100, PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI)
        lataa(nykyinen)
        whenever(pituuslaskuri.calculateHyvaksiluettavatDaysLeft(any(), any())).thenAnswer {
            HyvaksiluettavatCounterData(hyvaksiluettavatPerYearMap = mutableMapOf(2020 to 0.0, 2021 to 0.0))
        }

        repeat(2) { assertThat(tarkistaUusi(50)).isTrue() }

        verify(pituuslaskuri, times(2)).calculateHyvaksiluettavatDaysLeft(any(), eq(alku.minusDays(1)))
    }

    @ParameterizedTest
    @ValueSource(ints = [57, 43])
    fun `perakkaisten Double vahennysten jarjestys sailyy sadan prosentin rajalla`(ensimmainenProsentti: Int) {
        val ensimmainen = jakso(1, 100)
        val toinen = jakso(2, 100)
        lisaaPoissaolo(ensimmainen, ensimmainenProsentti, PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN)
        lisaaPoissaolo(toinen, 100 - ensimmainenProsentti, PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN)
        lataa(ensimmainen, toinen)
        val ehdotus = TyoskentelyjaksoDTO(id = 1, alkamispaiva = alku, paattymispaiva = loppu, osaaikaprosentti = 100)

        // Nykyinen laskenta: 2.0 - 0.57 - 0.43 > 1.0, mutta 2.0 - 0.43 - 0.57 == 1.0.
        assertThat(validaattori.validateTyoskentelyjakso(11, ehdotus)).isEqualTo(ensimmainenProsentti == 43)
    }

    private fun tarkistaUusi(osaaikaprosentti: Int) = validaattori.validateTyoskentelyjakso(
        11, TyoskentelyjaksoDTO(alkamispaiva = alku, paattymispaiva = loppu, osaaikaprosentti = osaaikaprosentti)
    )

    private fun jakso(id: Long, osaaikaprosentti: Int) = Tyoskentelyjakso(
        id = id, alkamispaiva = alku, paattymispaiva = loppu, osaaikaprosentti = osaaikaprosentti
    )

    private fun lataa(vararg aineisto: Tyoskentelyjakso) {
        whenever(jaksot.findAllByOpintooikeusUntilDateEagerWithRelationships(eq(11L), any())).thenReturn(aineisto.toList())
    }

    private fun lisaaPoissaolo(jakso: Tyoskentelyjakso, prosentti: Int, tyyppi: PoissaolonSyyTyyppi) {
        jakso.keskeytykset.add(Keskeytysaika(
            id = jakso.id, alkamispaiva = alku, paattymispaiva = loppu, poissaoloprosentti = prosentti,
            poissaolonSyy = PoissaolonSyy(id = 1, vahennystyyppi = tyyppi), tyoskentelyjakso = jakso
        ))
    }
}
