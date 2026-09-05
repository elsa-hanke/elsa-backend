package fi.elsapalvelu.elsa.service.arkistointi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArkistointitoimituksenTulosTest {

    @Test
    fun `virheen tiedot rajataan tietomallin enimmäispituuksiin`() {
        val virhe = Arkistointivirhe.luo(
            koodi = "K".repeat(Arkistointivirhe.VIRHEKOODIN_ENIMMAISPITUUS + 1),
            kuvaus = "V".repeat(Arkistointivirhe.VIRHEKUVAUKSEN_ENIMMAISPITUUS + 1)
        )

        assertThat(virhe.koodi).hasSize(Arkistointivirhe.VIRHEKOODIN_ENIMMAISPITUUS)
        assertThat(virhe.kuvaus).hasSize(Arkistointivirhe.VIRHEKUVAUKSEN_ENIMMAISPITUUS)
    }

    @Test
    fun `virhekoodi ei saa olla tyhja`() {
        assertThrows<IllegalArgumentException> {
            Arkistointivirhe.luo(" ", "Virhe")
        }
    }

    @Test
    fun `tulosmalli erottaa onnistumisen ja virhetyypit`() {
        val virhe = Arkistointivirhe.luo("ARKISTO_EI_SAATAVILLA", "Palvelu ei vastaa")
        val tulokset = listOf(
            ArkistointitoimituksenTulos.Onnistui("toimitus-1"),
            ArkistointitoimituksenTulos.TilapainenVirhe(virhe),
            ArkistointitoimituksenTulos.PysyvaVirhe(virhe)
        )

        assertThat(tulokset).hasOnlyElementsOfTypes(
            ArkistointitoimituksenTulos.Onnistui::class.java,
            ArkistointitoimituksenTulos.TilapainenVirhe::class.java,
            ArkistointitoimituksenTulos.PysyvaVirhe::class.java
        )
    }
}
