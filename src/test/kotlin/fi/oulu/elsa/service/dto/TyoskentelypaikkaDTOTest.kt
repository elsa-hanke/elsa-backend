package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TyoskentelypaikkaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(TyoskentelypaikkaDTO::class)
        val tyoskentelypaikkaDTO1 = TyoskentelypaikkaDTO()
        tyoskentelypaikkaDTO1.id = 1L
        val tyoskentelypaikkaDTO2 = TyoskentelypaikkaDTO()
        assertThat(tyoskentelypaikkaDTO1).isNotEqualTo(tyoskentelypaikkaDTO2)
        tyoskentelypaikkaDTO2.id = tyoskentelypaikkaDTO1.id
        assertThat(tyoskentelypaikkaDTO1).isEqualTo(tyoskentelypaikkaDTO2)
        tyoskentelypaikkaDTO2.id = 2L
        assertThat(tyoskentelypaikkaDTO1).isNotEqualTo(tyoskentelypaikkaDTO2)
        tyoskentelypaikkaDTO1.id = null
        assertThat(tyoskentelypaikkaDTO1).isNotEqualTo(tyoskentelypaikkaDTO2)
    }
}
