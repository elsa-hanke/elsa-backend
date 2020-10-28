package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KouluttajavaltuutusDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KouluttajavaltuutusDTO::class)
        val kouluttajavaltuutusDTO1 = KouluttajavaltuutusDTO()
        kouluttajavaltuutusDTO1.id = 1L
        val kouluttajavaltuutusDTO2 = KouluttajavaltuutusDTO()
        assertThat(kouluttajavaltuutusDTO1).isNotEqualTo(kouluttajavaltuutusDTO2)
        kouluttajavaltuutusDTO2.id = kouluttajavaltuutusDTO1.id
        assertThat(kouluttajavaltuutusDTO1).isEqualTo(kouluttajavaltuutusDTO2)
        kouluttajavaltuutusDTO2.id = 2L
        assertThat(kouluttajavaltuutusDTO1).isNotEqualTo(kouluttajavaltuutusDTO2)
        kouluttajavaltuutusDTO1.id = null
        assertThat(kouluttajavaltuutusDTO1).isNotEqualTo(kouluttajavaltuutusDTO2)
    }
}
