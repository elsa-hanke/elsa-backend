package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.ElsaBackendApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

class LocalDateExtensionsTest {

    @Test
    fun `duringYears should return list of years between dates`() {
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)

        val years = startDate.duringYears(endDate)

        assertThat(years).containsExactly(2020, 2021, 2022)
    }

    @Test
    fun `duringYears should return single year when same year`() {
        val startDate = LocalDate.of(2020, 3, 15)
        val endDate = LocalDate.of(2020, 9, 20)

        val years = startDate.duringYears(endDate)

        assertThat(years).containsExactly(2020)
    }

    @Test
    fun `daysBetween should calculate inclusive days`() {
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 1, 10)

        val days = startDate.daysBetween(endDate)

        assertThat(days).isEqualTo(10)
    }

    @Test
    fun `daysBetween should return 1 for same day`() {
        val date = LocalDate.of(2020, 1, 1)

        val days = date.daysBetween(date)

        assertThat(days).isEqualTo(1)
    }

    @Test
    fun `isInRange should return true when date is in range`() {
        val date = LocalDate.of(2020, 6, 15)
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 31)

        assertThat(date.isInRange(startDate, endDate)).isTrue
    }

    @Test
    fun `isInRange should return true when date equals start date`() {
        val date = LocalDate.of(2020, 1, 1)
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 31)

        assertThat(date.isInRange(startDate, endDate)).isTrue
    }

    @Test
    fun `isInRange should return true when date equals end date`() {
        val date = LocalDate.of(2020, 12, 31)
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 31)

        assertThat(date.isInRange(startDate, endDate)).isTrue
    }

    @Test
    fun `isInRange should return false when date is before range`() {
        val date = LocalDate.of(2019, 12, 31)
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 31)

        assertThat(date.isInRange(startDate, endDate)).isFalse
    }

    @Test
    fun `isInRange should return false when date is after range`() {
        val date = LocalDate.of(2021, 1, 1)
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 31)

        assertThat(date.isInRange(startDate, endDate)).isFalse
    }

    @Test
    fun `isInRange should return true when endDate is null and date is after start`() {
        val date = LocalDate.of(2020, 6, 15)
        val startDate = LocalDate.of(2020, 1, 1)

        assertThat(date.isInRange(startDate, null)).isTrue
    }

    @Test
    fun `periodLessThan should return true when period is less than specified years`() {
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2021, 6, 1)

        assertThat(startDate.periodLessThan(endDate, 2)).isTrue
    }

    @Test
    fun `periodLessThan should return false when period equals or exceeds years`() {
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2023, 1, 1)

        assertThat(startDate.periodLessThan(endDate, 3)).isFalse
    }

    @Test
    fun `startOfYearDate should return January 1st`() {
        val date = 2020.startOfYearDate()

        assertThat(date).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `endOfYearDate should return December 31st`() {
        val date = 2020.endOfYearDate()

        assertThat(date).isEqualTo(LocalDate.of(2020, 12, 31))
    }

    @Test
    fun `endOfYearDate should handle leap year`() {
        val date = 2020.endOfYearDate()

        assertThat(date.year).isEqualTo(2020)
        assertThat(date.monthValue).isEqualTo(12)
        assertThat(date.dayOfMonth).isEqualTo(31)
    }
}

