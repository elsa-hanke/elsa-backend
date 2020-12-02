package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KeskeytysaikaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Keskeytysaika::class)
        val keskeytysaika1 = Keskeytysaika()
        keskeytysaika1.id = 1L
        val keskeytysaika2 = Keskeytysaika()
        keskeytysaika2.id = keskeytysaika1.id
        assertThat(keskeytysaika1).isEqualTo(keskeytysaika2)
        keskeytysaika2.id = 2L
        assertThat(keskeytysaika1).isNotEqualTo(keskeytysaika2)
        keskeytysaika1.id = null
        assertThat(keskeytysaika1).isNotEqualTo(keskeytysaika2)
    }
}
