package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoulutusjaksoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KoulutusjaksoDTO::class)
        val koulutusjaksoDTO1 = KoulutusjaksoDTO()
        koulutusjaksoDTO1.id = 1L
        val koulutusjaksoDTO2 = KoulutusjaksoDTO()
        assertThat(koulutusjaksoDTO1).isNotEqualTo(koulutusjaksoDTO2)
        koulutusjaksoDTO2.id = koulutusjaksoDTO1.id
        assertThat(koulutusjaksoDTO1).isEqualTo(koulutusjaksoDTO2)
        koulutusjaksoDTO2.id = 2L
        assertThat(koulutusjaksoDTO1).isNotEqualTo(koulutusjaksoDTO2)
        koulutusjaksoDTO1.id = null
        assertThat(koulutusjaksoDTO1).isNotEqualTo(koulutusjaksoDTO2)
    }
}
