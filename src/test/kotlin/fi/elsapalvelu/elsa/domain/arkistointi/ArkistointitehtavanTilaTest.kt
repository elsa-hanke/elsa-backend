package fi.elsapalvelu.elsa.domain.arkistointi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArkistointitehtavanTilaTest {

    @Test
    fun `sallitut tilasiirtymat on maaritelty`() {
        val sallitutSiirtymat = setOf(
            ArkistointitehtavanTila.ODOTTAA to ArkistointitehtavanTila.KASITTELYSSA,
            ArkistointitehtavanTila.KASITTELYSSA to ArkistointitehtavanTila.ODOTTAA,
            ArkistointitehtavanTila.KASITTELYSSA to ArkistointitehtavanTila.LAHETETTY,
            ArkistointitehtavanTila.KASITTELYSSA to ArkistointitehtavanTila.VAATII_TOIMENPITEITA,
            ArkistointitehtavanTila.VAATII_TOIMENPITEITA to ArkistointitehtavanTila.ODOTTAA
        )

        ArkistointitehtavanTila.entries.forEach { nykyinen ->
            ArkistointitehtavanTila.entries.forEach { uusi ->
                assertThat(nykyinen.salliiSiirtyman(uusi))
                    .describedAs("siirtyma $nykyinen -> $uusi")
                    .isEqualTo(nykyinen to uusi in sallitutSiirtymat)
            }
        }
    }

    @Test
    fun `tehtava siirtyy sallittuun tilaan`() {
        val tehtava = Arkistointitehtava()

        tehtava.siirryTilaan(ArkistointitehtavanTila.KASITTELYSSA)
        tehtava.siirryTilaan(ArkistointitehtavanTila.LAHETETTY)

        assertThat(tehtava.tila).isEqualTo(ArkistointitehtavanTila.LAHETETTY)
    }

    @Test
    fun `tehtava ei siirry kiellettyyn tilaan`() {
        val tehtava = Arkistointitehtava()

        assertThrows<IllegalArgumentException> {
            tehtava.siirryTilaan(ArkistointitehtavanTila.LAHETETTY)
        }

        assertThat(tehtava.tila).isEqualTo(ArkistointitehtavanTila.ODOTTAA)
    }
}
