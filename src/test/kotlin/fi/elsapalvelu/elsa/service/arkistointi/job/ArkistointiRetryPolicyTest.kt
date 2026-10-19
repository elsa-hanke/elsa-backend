package fi.elsapalvelu.elsa.service.arkistointi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class ArkistointiRetryPolicyTest {
    @Test
    fun `delay grows exponentially and remains capped even for a large failure count`() {
        val policy = ArkistointiRetryPolicy().apply {
            initialDelay = Duration.ofSeconds(2)
            maxDelay = Duration.ofSeconds(10)
        }
        policy.validate()
        assertThat((0..4).map(policy::delay)).containsExactly(
            Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8),
            Duration.ofSeconds(10), Duration.ofSeconds(10)
        )
        assertThat(policy.delay(Int.MAX_VALUE)).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `invalid limits and delays fail validation`() {
        val invalid = listOf<ArkistointiRetryPolicy.() -> Unit>(
            { maxAttempts = 0 }, { initialDelay = Duration.ZERO },
            { maxDelay = Duration.ofSeconds(1) }, { multiplier = Double.NaN }, { multiplier = 0.5 }
        )
        invalid.forEach { configure ->
            assertThrows<IllegalArgumentException> { ArkistointiRetryPolicy().apply(configure).validate() }
        }
    }
}
