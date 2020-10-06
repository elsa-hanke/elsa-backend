package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArvioitavaOsaalueTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(ArvioitavaOsaalue::class)
        val arvioitavaOsaalue1 = ArvioitavaOsaalue()
        arvioitavaOsaalue1.id = 1L
        val arvioitavaOsaalue2 = ArvioitavaOsaalue()
        arvioitavaOsaalue2.id = arvioitavaOsaalue1.id
        assertThat(arvioitavaOsaalue1).isEqualTo(arvioitavaOsaalue2)
        arvioitavaOsaalue2.id = 2L
        assertThat(arvioitavaOsaalue1).isNotEqualTo(arvioitavaOsaalue2)
        arvioitavaOsaalue1.id = null
        assertThat(arvioitavaOsaalue1).isNotEqualTo(arvioitavaOsaalue2)
    }
}
