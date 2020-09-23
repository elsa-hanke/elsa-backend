package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TyoskentelyjaksoTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Tyoskentelyjakso::class)
        val tyoskentelyjakso1 = Tyoskentelyjakso()
        tyoskentelyjakso1.id = 1L
        val tyoskentelyjakso2 = Tyoskentelyjakso()
        tyoskentelyjakso2.id = tyoskentelyjakso1.id
        assertThat(tyoskentelyjakso1).isEqualTo(tyoskentelyjakso2)
        tyoskentelyjakso2.id = 2L
        assertThat(tyoskentelyjakso1).isNotEqualTo(tyoskentelyjakso2)
        tyoskentelyjakso1.id = null
        assertThat(tyoskentelyjakso1).isNotEqualTo(tyoskentelyjakso2)
    }
}
