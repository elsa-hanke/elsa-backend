package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KayttajaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KayttajaDTO::class)
        val kayttajaDTO1 = KayttajaDTO()
        kayttajaDTO1.id = "b3863335-71c8-4e0d-8c45-82f06991e285"
        val kayttajaDTO2 = KayttajaDTO()
        assertThat(kayttajaDTO1).isNotEqualTo(kayttajaDTO2)
        kayttajaDTO2.id = kayttajaDTO1.id
        assertThat(kayttajaDTO1).isEqualTo(kayttajaDTO2)
        kayttajaDTO2.id = "3cd55361-7cc7-4c65-8923-a363e8d49faa"
        assertThat(kayttajaDTO1).isNotEqualTo(kayttajaDTO2)
        kayttajaDTO1.id = null
        assertThat(kayttajaDTO1).isNotEqualTo(kayttajaDTO2)
    }
}
