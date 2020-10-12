package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsaamisalueenArviointiTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(OsaamisalueenArviointi::class)
        val osaamisalueenArviointi1 = OsaamisalueenArviointi()
        osaamisalueenArviointi1.id = 1L
        val osaamisalueenArviointi2 = OsaamisalueenArviointi()
        osaamisalueenArviointi2.id = osaamisalueenArviointi1.id
        assertThat(osaamisalueenArviointi1).isEqualTo(osaamisalueenArviointi2)
        osaamisalueenArviointi2.id = 2L
        assertThat(osaamisalueenArviointi1).isNotEqualTo(osaamisalueenArviointi2)
        osaamisalueenArviointi1.id = null
        assertThat(osaamisalueenArviointi1).isNotEqualTo(osaamisalueenArviointi2)
    }
}
