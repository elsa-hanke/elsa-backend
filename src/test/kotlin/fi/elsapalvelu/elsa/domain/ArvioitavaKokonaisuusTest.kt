package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArvioitavaKokonaisuusTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(ArvioitavaKokonaisuus::class)
        val arvioitavaKokonaisuus1 = ArvioitavaKokonaisuus()
        arvioitavaKokonaisuus1.id = 1L
        val arvioitavaKokonaisuus2 = ArvioitavaKokonaisuus()
        arvioitavaKokonaisuus2.id = arvioitavaKokonaisuus1.id
        assertThat(arvioitavaKokonaisuus1).isEqualTo(arvioitavaKokonaisuus2)
        arvioitavaKokonaisuus2.id = 2L
        assertThat(arvioitavaKokonaisuus1).isNotEqualTo(arvioitavaKokonaisuus2)
        arvioitavaKokonaisuus1.id = null
        assertThat(arvioitavaKokonaisuus1).isNotEqualTo(arvioitavaKokonaisuus2)
    }
}
