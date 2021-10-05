package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class KoulutussuunnitelmaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KoulutussuunnitelmaDTO::class)
        val koulutussuunnitelmaDTO1 = KoulutussuunnitelmaDTO()
        koulutussuunnitelmaDTO1.id = 1L
        val koulutussuunnitelmaDTO2 = KoulutussuunnitelmaDTO()
        Assertions.assertThat(koulutussuunnitelmaDTO1).isNotEqualTo(koulutussuunnitelmaDTO2)
        koulutussuunnitelmaDTO2.id = koulutussuunnitelmaDTO1.id
        Assertions.assertThat(koulutussuunnitelmaDTO1).isEqualTo(koulutussuunnitelmaDTO2)
        koulutussuunnitelmaDTO2.id = 2L
        Assertions.assertThat(koulutussuunnitelmaDTO1).isNotEqualTo(koulutussuunnitelmaDTO2)
        koulutussuunnitelmaDTO1.id = null
        Assertions.assertThat(koulutussuunnitelmaDTO1).isNotEqualTo(koulutussuunnitelmaDTO2)
    }
}
