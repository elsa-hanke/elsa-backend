package fi.elsapalvelu.elsa.domain.kayttaja

import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OpintooikeudenTilaTest {

    @ParameterizedTest
    @CsvSource(
        "ACTIVE, AKTIIVINEN",
        "ACTIVE_NONATTENDING, AKTIIVINEN_EI_LASNA",
        "NOT_STARTED, PASSIIVINEN",
        "PASSIVE, PASSIIVINEN",
        "GRADUATED, VALMISTUNUT",
        "RESCINDED, PERUUTETTU",
        "CANCELLED_BY_ADMINISTRATION, PERUUTETTU",
        "EXPIRED, VANHENTUNUT",
        "TERMINATED,",
        "PASSIVATED,"
    )
    fun `Sisu enum and string statuses map consistently`(
        source: SisuOpintooikeudenTila,
        expected: OpintooikeudenTila?
    ) {
        assertThat(OpintooikeudenTila.fromSisuOpintooikeudenTila(source)).isEqualTo(expected)
        assertThat(OpintooikeudenTila.fromSisuOpintooikeudenTilaStr(source.name)).isEqualTo(expected)
    }

    @Test
    fun `missing Sisu status remains null`() {
        assertThat(OpintooikeudenTila.fromSisuOpintooikeudenTila(null)).isNull()
        assertThat(OpintooikeudenTila.fromSisuOpintooikeudenTilaStr(null)).isNull()
    }
}
