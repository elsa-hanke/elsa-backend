package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TyoskentelypaikkaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Tyoskentelypaikka::class)
        val tyoskentelypaikka1 = Tyoskentelypaikka()
        tyoskentelypaikka1.id = 1L
        val tyoskentelypaikka2 = Tyoskentelypaikka()
        tyoskentelypaikka2.id = tyoskentelypaikka1.id
        assertThat(tyoskentelypaikka1).isEqualTo(tyoskentelypaikka2)
        tyoskentelypaikka2.id = 2L
        assertThat(tyoskentelypaikka1).isNotEqualTo(tyoskentelypaikka2)
        tyoskentelypaikka1.id = null
        assertThat(tyoskentelypaikka1).isNotEqualTo(tyoskentelypaikka2)
    }
}
