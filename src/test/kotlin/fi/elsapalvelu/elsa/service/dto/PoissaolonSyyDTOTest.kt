package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PoissaolonSyyDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(PoissaolonSyyDTO::class)
        val poissaolonSyyDTO1 = PoissaolonSyyDTO()
        poissaolonSyyDTO1.id = 1L
        val poissaolonSyyDTO2 = PoissaolonSyyDTO()
        assertThat(poissaolonSyyDTO1).isNotEqualTo(poissaolonSyyDTO2)
        poissaolonSyyDTO2.id = poissaolonSyyDTO1.id
        assertThat(poissaolonSyyDTO1).isEqualTo(poissaolonSyyDTO2)
        poissaolonSyyDTO2.id = 2L
        assertThat(poissaolonSyyDTO1).isNotEqualTo(poissaolonSyyDTO2)
        poissaolonSyyDTO1.id = null
        assertThat(poissaolonSyyDTO1).isNotEqualTo(poissaolonSyyDTO2)
    }
}
