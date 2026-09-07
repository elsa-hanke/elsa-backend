package fi.elsapalvelu.elsa.service.arkistointi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArkistointiResultTest {

    @Test
    fun `virheen tiedot rajataan tietomallin enimmaispituuksiin`() {
        val error = ArkistointiError.create(
            code = "K".repeat(ArkistointiError.MAX_ERROR_CODE_LENGTH + 1),
            description = "V".repeat(ArkistointiError.MAX_ERROR_DESCRIPTION_LENGTH + 1)
        )

        assertThat(error.code).hasSize(ArkistointiError.MAX_ERROR_CODE_LENGTH)
        assertThat(error.description).hasSize(ArkistointiError.MAX_ERROR_DESCRIPTION_LENGTH)
    }

    @Test
    fun `virhekoodi ei saa olla tyhja`() {
        assertThrows<IllegalArgumentException> {
            ArkistointiError.create(" ", "Virhe")
        }
    }

    @Test
    fun `tulosmalli erottaa onnistumisen ja virhetyypit`() {
        val error = ArkistointiError.create("ARKISTO_EI_SAATAVILLA", "Palvelu ei vastaa")
        val tulokset = listOf(
            ArkistointiResult.Success("toimitus-1"),
            ArkistointiResult.RetryableFailure(error),
            ArkistointiResult.PermanentFailure(error)
        )

        assertThat(tulokset).hasOnlyElementsOfTypes(
            ArkistointiResult.Success::class.java,
            ArkistointiResult.RetryableFailure::class.java,
            ArkistointiResult.PermanentFailure::class.java
        )
    }
}
