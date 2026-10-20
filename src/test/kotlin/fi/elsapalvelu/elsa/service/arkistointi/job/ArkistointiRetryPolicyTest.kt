package fi.elsapalvelu.elsa.service.arkistointi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ClassPathResource
import java.time.Duration

class ArkistointiRetryPolicyTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `default and configured policy allow 25 attempts within three days of planned delays`(loadYaml: Boolean) {
        val policy = ArkistointiRetryPolicy { 1.0 }
        if (loadYaml) {
            val environment = StandardEnvironment()
            YamlPropertySourceLoader().load("application", ClassPathResource("config/application.yml"))
                .forEach(environment.propertySources::addFirst)
            Binder.get(environment).bind("application.arkistointi.retry", Bindable.ofInstance(policy))
        }
        policy.validate()
        assertThat(policy.maxAttempts).isEqualTo(25)
        assertThat(policy.jitterFactor).isEqualTo(0.1)
        // The first execution is immediate: only the remaining 24 attempts incur a delay.
        val delays = (0 until policy.maxAttempts - 1).map(policy::delay)
        assertThat(delays.take(8)).containsExactly(
            Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(4), Duration.ofMinutes(8),
            Duration.ofMinutes(16), Duration.ofMinutes(32), Duration.ofMinutes(64), Duration.ofMinutes(128)
        )
        assertThat(delays.drop(8)).hasSize(16).containsOnly(Duration.ofHours(4))
        val total = delays.fold(Duration.ZERO, Duration::plus)
        assertThat(total).isEqualTo(Duration.ofHours(68).plusMinutes(15)).isLessThan(Duration.ofDays(3))
    }

    @Test
    fun `delay grows exponentially and remains capped even for a large failure count`() {
        val policy = ArkistointiRetryPolicy().apply {
            initialDelay = Duration.ofSeconds(2)
            maxDelay = Duration.ofSeconds(10)
            jitterFactor = 0.0
        }
        policy.validate()
        assertThat((0..4).map(policy::delay)).containsExactly(
            Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8),
            Duration.ofSeconds(10), Duration.ofSeconds(10)
        )
        assertThat(policy.delay(Int.MAX_VALUE)).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `disabled jitter uses the exact exponential delay without sampling randomness`() {
        val policy = ArkistointiRetryPolicy { error("Randomness must not be used when jitter is disabled") }
            .apply { jitterFactor = 0.0 }
        assertThat(policy.delay(0)).isEqualTo(Duration.ofMinutes(1))
        assertThat(policy.delay(Int.MAX_VALUE)).isEqualTo(Duration.ofHours(4))
    }

    @ParameterizedTest
    @CsvSource("0.0,54,12960", "0.5,57,13680", "1.0,60,14400")
    fun `jitter is bounded before and after the exponential delay reaches the cap`(
        sample: Double,
        initialSeconds: Long,
        cappedSeconds: Long
    ) {
        val policy = ArkistointiRetryPolicy { sample }
        policy.validate()
        assertThat(policy.delay(0)).isEqualTo(Duration.ofSeconds(initialSeconds))
        assertThat(policy.delay(Int.MAX_VALUE)).isEqualTo(Duration.ofSeconds(cappedSeconds))
        val total = (0 until policy.maxAttempts - 1).map(policy::delay).fold(Duration.ZERO, Duration::plus)
        assertThat(total).isBetween(Duration.ofHours(61).plusMinutes(25).plusSeconds(30), Duration.ofHours(68).plusMinutes(15))
    }

    @Test
    fun `each retry samples anew and millisecond delays stay positive`() {
        val samples = listOf(0.0, 1.0).iterator()
        val policy = ArkistointiRetryPolicy { samples.next() }
        assertThat(policy.delay(0)).isEqualTo(Duration.ofSeconds(54))
        assertThat(policy.delay(0)).isEqualTo(Duration.ofMinutes(1))
        val tinyDelay = ArkistointiRetryPolicy { 0.0 }.apply {
            initialDelay = Duration.ofMillis(1)
            maxDelay = Duration.ofMillis(1)
            jitterFactor = 0.99
        }
        tinyDelay.validate()
        assertThat(tinyDelay.delay(0)).isEqualTo(Duration.ofMillis(1))
    }

    @Test
    fun `invalid limits and delays fail validation`() {
        val invalid = listOf<ArkistointiRetryPolicy.() -> Unit>(
            { maxAttempts = 0 }, { initialDelay = Duration.ZERO },
            { maxDelay = Duration.ofSeconds(1) }, { multiplier = Double.NaN }, { multiplier = 0.5 },
            { jitterFactor = -0.1 }, { jitterFactor = 1.0 }, { jitterFactor = Double.NaN }, { jitterFactor = Double.POSITIVE_INFINITY }
        )
        invalid.forEach { configure ->
            assertThrows<IllegalArgumentException> { ArkistointiRetryPolicy().apply(configure).validate() }
        }
    }
}
