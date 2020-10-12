package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsaamisenArviointiTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(OsaamisenArviointi::class)
        val osaamisenArviointi1 = OsaamisenArviointi()
        osaamisenArviointi1.id = 1L
        val osaamisenArviointi2 = OsaamisenArviointi()
        osaamisenArviointi2.id = osaamisenArviointi1.id
        assertThat(osaamisenArviointi1).isEqualTo(osaamisenArviointi2)
        osaamisenArviointi2.id = 2L
        assertThat(osaamisenArviointi1).isNotEqualTo(osaamisenArviointi2)
        osaamisenArviointi1.id = null
        assertThat(osaamisenArviointi1).isNotEqualTo(osaamisenArviointi2)
    }
}
