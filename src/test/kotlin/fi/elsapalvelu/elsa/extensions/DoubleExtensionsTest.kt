package fi.elsapalvelu.elsa.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DoubleExtensionsTest {

    @Test
    fun `toYears should convert days to years`() {
        assertThat(365.0.toYears()).isEqualTo(1)
        assertThat(730.0.toYears()).isEqualTo(2)
        assertThat(0.0.toYears()).isEqualTo(0)
        assertThat(364.0.toYears()).isEqualTo(0)
        assertThat(1095.0.toYears()).isEqualTo(3)
    }

    @Test
    fun `toMonths should convert remaining days to months`() {
        assertThat(30.0.toMonths()).isEqualTo(0)
        assertThat(365.0.toMonths()).isEqualTo(0)
        assertThat(395.0.toMonths()).isEqualTo(0)
        assertThat(730.0.toMonths()).isEqualTo(0)
    }

    @Test
    fun `toMonthsDouble should return accurate month calculation`() {
        assertThat(365.0.toMonthsDouble()).isEqualTo(0.0)
        assertThat(0.0.toMonthsDouble()).isEqualTo(0.0)
    }

    @Test
    fun `toDays should convert remaining to days`() {
        assertThat(0.0.toDays()).isEqualTo(0)
        assertThat(365.0.toDays()).isEqualTo(0)
        assertThat(29.0.toDays()).isEqualTo(29)
        assertThat(30.0.toDays()).isEqualTo(30)
        assertThat(31.0.toDays()).isEqualTo(0)
    }

    @Test
    fun `combined conversion should handle complex periods`() {
        val days = 400.0
        val years = days.toYears()
        val months = days.toMonths()

        assertThat(years).isEqualTo(1)
        assertThat(months).isEqualTo(1)
    }
}

