package fi.elsapalvelu.elsa.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class KoulutussuunnitelmaTest {

    @Test
    fun equalsVerifier() {
        fi.elsapalvelu.elsa.web.rest.equalsVerifier(Koulutussuunnitelma::class)
        val koulutussuunnitelma1 = Koulutussuunnitelma()
        koulutussuunnitelma1.id = 1L
        val koulutussuunnitelma2 = Koulutussuunnitelma()
        koulutussuunnitelma2.id = koulutussuunnitelma1.id
        Assertions.assertThat(koulutussuunnitelma1).isEqualTo(koulutussuunnitelma2)
        koulutussuunnitelma2.id = 2L
        Assertions.assertThat(koulutussuunnitelma1).isNotEqualTo(koulutussuunnitelma2)
        koulutussuunnitelma1.id = null
        Assertions.assertThat(koulutussuunnitelma1).isNotEqualTo(koulutussuunnitelma2)
    }
}
