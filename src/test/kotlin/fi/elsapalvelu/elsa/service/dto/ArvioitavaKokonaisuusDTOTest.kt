package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArvioitavaKokonaisuusDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(ArvioitavaKokonaisuusDTO::class)
        val arvioitavaKokonaisuusDTO1 = ArvioitavaKokonaisuusDTO()
        arvioitavaKokonaisuusDTO1.id = 1L
        val arvioitavaKokonaisuusDTO2 = ArvioitavaKokonaisuusDTO()
        assertThat(arvioitavaKokonaisuusDTO1).isNotEqualTo(arvioitavaKokonaisuusDTO2)
        arvioitavaKokonaisuusDTO2.id = arvioitavaKokonaisuusDTO1.id
        assertThat(arvioitavaKokonaisuusDTO1).isEqualTo(arvioitavaKokonaisuusDTO2)
        arvioitavaKokonaisuusDTO2.id = 2L
        assertThat(arvioitavaKokonaisuusDTO1).isNotEqualTo(arvioitavaKokonaisuusDTO2)
        arvioitavaKokonaisuusDTO1.id = null
        assertThat(arvioitavaKokonaisuusDTO1).isNotEqualTo(arvioitavaKokonaisuusDTO2)
    }
}
