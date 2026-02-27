package fi.elsapalvelu.elsa.service.criteria

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter

class NimiErikoisalaAndAvoinCriteriaTest {

    @Test
    fun `should create criteria with all null values by default`() {
        val criteria = NimiErikoisalaAndAvoinCriteria()

        assertThat(criteria.erikoistujanNimi).isNull()
        assertThat(criteria.erikoisalaId).isNull()
        assertThat(criteria.avoin).isNull()
    }

    @Test
    fun `should create criteria with specified values`() {
        val nimiFilter = StringFilter().apply { contains = "Matti" }
        val erikoisalaFilter = LongFilter().apply { equals = 1L }

        val criteria = NimiErikoisalaAndAvoinCriteria(
            erikoistujanNimi = nimiFilter,
            erikoisalaId = erikoisalaFilter,
            avoin = true
        )

        assertThat(criteria.erikoistujanNimi?.contains).isEqualTo("Matti")
        assertThat(criteria.erikoisalaId?.equals).isEqualTo(1L)
        assertThat(criteria.avoin).isTrue
    }

    @Test
    fun `copy constructor should create independent copy`() {
        val nimiFilter = StringFilter().apply { contains = "Teppo" }
        val erikoisalaFilter = LongFilter().apply { equals = 2L }

        val original = NimiErikoisalaAndAvoinCriteria(
            erikoistujanNimi = nimiFilter,
            erikoisalaId = erikoisalaFilter,
            avoin = false
        )

        val copy = NimiErikoisalaAndAvoinCriteria(original)

        assertThat(copy.erikoistujanNimi?.contains).isEqualTo("Teppo")
        assertThat(copy.erikoisalaId?.equals).isEqualTo(2L)
        assertThat(copy.avoin).isFalse

        // Verify deep copy - modifying copy should not affect original
        copy.erikoistujanNimi?.contains = "Modified"
        assertThat(original.erikoistujanNimi?.contains).isEqualTo("Teppo")
    }

    @Test
    fun `copy method should create independent copy`() {
        val criteria = NimiErikoisalaAndAvoinCriteria(
            erikoistujanNimi = StringFilter().apply { contains = "Test" },
            erikoisalaId = LongFilter().apply { equals = 5L },
            avoin = true
        )

        val copy = criteria.copy()

        assertThat(copy).isNotSameAs(criteria)
        assertThat(copy.erikoistujanNimi?.contains).isEqualTo("Test")
        assertThat(copy.erikoisalaId?.equals).isEqualTo(5L)
        assertThat(copy.avoin).isTrue
    }

    @Test
    fun `should handle null filters in copy`() {
        val criteria = NimiErikoisalaAndAvoinCriteria(
            erikoistujanNimi = null,
            erikoisalaId = null,
            avoin = null
        )

        val copy = criteria.copy()

        assertThat(copy.erikoistujanNimi).isNull()
        assertThat(copy.erikoisalaId).isNull()
        assertThat(copy.avoin).isNull()
    }

    @Test
    fun `should set erikoistujanNimi filter`() {
        val criteria = NimiErikoisalaAndAvoinCriteria()
        val filter = StringFilter().apply {
            contains = "Virtanen"
        }

        criteria.erikoistujanNimi = filter

        assertThat(criteria.erikoistujanNimi?.contains).isEqualTo("Virtanen")
    }

    @Test
    fun `should set erikoisalaId filter`() {
        val criteria = NimiErikoisalaAndAvoinCriteria()
        val filter = LongFilter().apply {
            equals = 10L
        }

        criteria.erikoisalaId = filter

        assertThat(criteria.erikoisalaId?.equals).isEqualTo(10L)
    }

    @Test
    fun `should set avoin boolean`() {
        val criteria = NimiErikoisalaAndAvoinCriteria()

        criteria.avoin = true
        assertThat(criteria.avoin).isTrue

        criteria.avoin = false
        assertThat(criteria.avoin).isFalse
    }

    @Test
    fun `should support erikoisalaId with in filter`() {
        val criteria = NimiErikoisalaAndAvoinCriteria()
        val filter = LongFilter().apply {
            `in` = mutableListOf(1L, 2L, 3L)
        }

        criteria.erikoisalaId = filter

        assertThat(criteria.erikoisalaId?.`in`).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `should support erikoistujanNimi with different string operations`() {
        val criteria = NimiErikoisalaAndAvoinCriteria()
        val filter = StringFilter().apply {
            contains = "Search"
            equals = "ExactMatch"
        }

        criteria.erikoistujanNimi = filter

        assertThat(criteria.erikoistujanNimi?.contains).isEqualTo("Search")
        assertThat(criteria.erikoistujanNimi?.equals).isEqualTo("ExactMatch")
    }
}

