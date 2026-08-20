package fi.elsapalvelu.elsa.service.criteria

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter

class ErikoistujanEteneminenCriteriaTest {

    @Test
    fun `copy preserves every criterion and creates independent filters`() {
        val criteria = ErikoistujanEteneminenCriteria(
            nimi = StringFilter().apply { contains = "Matti" },
            erikoisalaId = LongFilter().apply { equals = 1L },
            asetusId = LongFilter().apply { equals = 2L },
            naytaPaattyneet = true
        )

        val copy = criteria.copy()

        assertThat(copy).isNotSameAs(criteria)
        assertThat(copy.nimi).isNotSameAs(criteria.nimi)
        assertThat(copy.erikoisalaId).isNotSameAs(criteria.erikoisalaId)
        assertThat(copy.asetusId).isNotSameAs(criteria.asetusId)
        assertThat(copy.nimi?.contains).isEqualTo("Matti")
        assertThat(copy.erikoisalaId?.equals).isEqualTo(1L)
        assertThat(copy.asetusId?.equals).isEqualTo(2L)
        assertThat(copy.naytaPaattyneet).isTrue()
    }
}
