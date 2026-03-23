package fi.elsapalvelu.elsa.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Period

class PeriodExtensionsTest {

    @Test
    fun `format should return 0 vrk for zero period`() {
        val period = Period.ZERO
        assertThat(period.format()).isEqualTo("0 vrk")
    }

    @Test
    fun `format should display years only`() {
        val period = Period.ofYears(3)
        assertThat(period.format()).isEqualTo("3v")
    }

    @Test
    fun `format should display months only`() {
        val period = Period.ofMonths(5)
        assertThat(period.format()).isEqualTo("5kk")
    }

    @Test
    fun `format should display days only`() {
        val period = Period.ofDays(15)
        assertThat(period.format()).isEqualTo("15pv")
    }

    @Test
    fun `format should display years and months`() {
        val period = Period.of(2, 6, 0)
        assertThat(period.format()).isEqualTo("2v 6kk")
    }

    @Test
    fun `format should display years and days`() {
        val period = Period.of(1, 0, 10)
        assertThat(period.format()).isEqualTo("1v 10pv")
    }

    @Test
    fun `format should display months and days`() {
        val period = Period.of(0, 3, 20)
        assertThat(period.format()).isEqualTo("3kk 20pv")
    }

    @Test
    fun `format should display complete period with years months and days`() {
        val period = Period.of(2, 4, 15)
        assertThat(period.format()).isEqualTo("2v 4kk 15pv")
    }

    @Test
    fun `format should handle single values correctly`() {
        assertThat(Period.of(1, 1, 1).format()).isEqualTo("1v 1kk 1pv")
    }
}

