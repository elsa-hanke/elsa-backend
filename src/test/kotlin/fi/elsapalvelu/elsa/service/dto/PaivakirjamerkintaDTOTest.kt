package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaivakirjamerkintaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(PaivakirjamerkintaDTO::class)
        val paivakirjamerkintaDTO1 = PaivakirjamerkintaDTO()
        paivakirjamerkintaDTO1.id = 1L
        val paivakirjamerkintaDTO2 = PaivakirjamerkintaDTO()
        assertThat(paivakirjamerkintaDTO1).isNotEqualTo(paivakirjamerkintaDTO2)
        paivakirjamerkintaDTO2.id = paivakirjamerkintaDTO1.id
        assertThat(paivakirjamerkintaDTO1).isEqualTo(paivakirjamerkintaDTO2)
        paivakirjamerkintaDTO2.id = 2L
        assertThat(paivakirjamerkintaDTO1).isNotEqualTo(paivakirjamerkintaDTO2)
        paivakirjamerkintaDTO1.id = null
        assertThat(paivakirjamerkintaDTO1).isNotEqualTo(paivakirjamerkintaDTO2)
    }
}
