package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KayttajaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Kayttaja::class)
        val kayttaja1 = Kayttaja()
        kayttaja1.id = "b3863335-71c8-4e0d-8c45-82f06991e285"
        val kayttaja2 = Kayttaja()
        kayttaja2.id = kayttaja1.id
        assertThat(kayttaja1).isEqualTo(kayttaja2)
        kayttaja2.id = "3cd55361-7cc7-4c65-8923-a363e8d49faa"
        assertThat(kayttaja1).isNotEqualTo(kayttaja2)
        kayttaja1.id = null
        assertThat(kayttaja1).isNotEqualTo(kayttaja2)
    }
}
