package fi.elsapalvelu.elsa.domain.arkistointi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArkistointiJobStatusTest {

    @Test
    fun `sallitut tilasiirtymat on maaritelty`() {
        val sallitutSiirtymat = setOf(
            ArkistointiJobStatus.ODOTTAA to ArkistointiJobStatus.KASITTELYSSA,
            ArkistointiJobStatus.KASITTELYSSA to ArkistointiJobStatus.ODOTTAA,
            ArkistointiJobStatus.KASITTELYSSA to ArkistointiJobStatus.LAHETETTY,
            ArkistointiJobStatus.KASITTELYSSA to ArkistointiJobStatus.VAATII_TOIMENPITEITA,
            ArkistointiJobStatus.VAATII_TOIMENPITEITA to ArkistointiJobStatus.ODOTTAA
        )

        ArkistointiJobStatus.entries.forEach { nykyinen ->
            ArkistointiJobStatus.entries.forEach { uusi ->
                assertThat(nykyinen.canTransitionTo(uusi))
                    .describedAs("siirtyma $nykyinen -> $uusi")
                    .isEqualTo(nykyinen to uusi in sallitutSiirtymat)
            }
        }
    }

    @Test
    fun `job siirtyy sallittuun tilaan`() {
        val job = ArkistointiJob()

        job.transitionTo(ArkistointiJobStatus.KASITTELYSSA)
        job.transitionTo(ArkistointiJobStatus.LAHETETTY)

        assertThat(job.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
    }

    @Test
    fun `job ei siirry kiellettyyn tilaan`() {
        val job = ArkistointiJob()

        assertThrows<IllegalArgumentException> {
            job.transitionTo(ArkistointiJobStatus.LAHETETTY)
        }

        assertThat(job.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
    }
}
