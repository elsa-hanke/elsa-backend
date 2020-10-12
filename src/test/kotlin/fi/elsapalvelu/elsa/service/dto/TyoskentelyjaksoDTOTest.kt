package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TyoskentelyjaksoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(TyoskentelyjaksoDTO::class)
        val tyoskentelyjaksoDTO1 = TyoskentelyjaksoDTO()
        tyoskentelyjaksoDTO1.id = 1L
        val tyoskentelyjaksoDTO2 = TyoskentelyjaksoDTO()
        assertThat(tyoskentelyjaksoDTO1).isNotEqualTo(tyoskentelyjaksoDTO2)
        tyoskentelyjaksoDTO2.id = tyoskentelyjaksoDTO1.id
        assertThat(tyoskentelyjaksoDTO1).isEqualTo(tyoskentelyjaksoDTO2)
        tyoskentelyjaksoDTO2.id = 2L
        assertThat(tyoskentelyjaksoDTO1).isNotEqualTo(tyoskentelyjaksoDTO2)
        tyoskentelyjaksoDTO1.id = null
        assertThat(tyoskentelyjaksoDTO1).isNotEqualTo(tyoskentelyjaksoDTO2)
    }
}
