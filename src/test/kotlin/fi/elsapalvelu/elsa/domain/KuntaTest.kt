package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KuntaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Kunta::class)
        val kunta1 = Kunta()
        kunta1.id = "1"
        val kunta2 = Kunta()
        kunta2.id = kunta1.id
        assertThat(kunta1).isEqualTo(kunta2)
        kunta2.id = "2"
        assertThat(kunta1).isNotEqualTo(kunta2)
        kunta1.id = null
        assertThat(kunta1).isNotEqualTo(kunta2)
    }
}
