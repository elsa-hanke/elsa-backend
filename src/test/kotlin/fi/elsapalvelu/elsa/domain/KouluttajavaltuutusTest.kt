package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KouluttajavaltuutusTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Kouluttajavaltuutus::class)
        val kouluttajavaltuutus1 = Kouluttajavaltuutus()
        kouluttajavaltuutus1.id = 1L
        val kouluttajavaltuutus2 = Kouluttajavaltuutus()
        kouluttajavaltuutus2.id = kouluttajavaltuutus1.id
        assertThat(kouluttajavaltuutus1).isEqualTo(kouluttajavaltuutus2)
        kouluttajavaltuutus2.id = 2L
        assertThat(kouluttajavaltuutus1).isNotEqualTo(kouluttajavaltuutus2)
        kouluttajavaltuutus1.id = null
        assertThat(kouluttajavaltuutus1).isNotEqualTo(kouluttajavaltuutus2)
    }
}
