package fi.elsapalvelu.elsa.service.arkistointi.job

import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import kotlin.math.pow

class ArkistointiRetryPolicy(private val randomSample: () -> Double = { ThreadLocalRandom.current().nextDouble() }) {
    /** Total attempts, including the first execution and interrupted executions. */
    var maxAttempts: Int = 25
    var initialDelay: Duration = Duration.ofMinutes(1)
    var maxDelay: Duration = Duration.ofHours(4)
    var multiplier: Double = 2.0
    /** Downward-only jitter: 0.1 chooses 90-100% of the capped exponential delay; 0 disables it. */
    var jitterFactor: Double = 0.1

    fun validate() {
        require(maxAttempts > 0) { "Archiving max-attempts must be positive" }
        require(initialDelay >= Duration.ofMillis(1)) { "Archiving initial-delay must be at least 1ms" }
        require(maxDelay >= initialDelay) { "Archiving max-delay must be at least initial-delay" }
        require(multiplier.isFinite() && multiplier >= 1.0) { "Archiving multiplier must be finite and at least 1" }
        require(jitterFactor.isFinite() && jitterFactor >= 0.0 && jitterFactor < 1.0) {
            "Archiving jitter-factor must be finite, at least 0 and less than 1"
        }
    }

    fun delay(previousFailures: Int): Duration {
        val millis = initialDelay.toMillis().toDouble() * multiplier.pow(previousFailures.coerceAtLeast(0))
        val cappedMillis = millis.coerceAtMost(maxDelay.toMillis().toDouble()).toLong()
        if (jitterFactor == 0.0) return Duration.ofMillis(cappedMillis)
        val sample = randomSample()
        require(sample.isFinite() && sample in 0.0..1.0) { "Archiving random sample must be between 0 and 1" }
        // Round the lower bound up so even a 1ms delay stays positive and inside the chosen range.
        val minimumMillis = ceil(cappedMillis.toDouble() * (1.0 - jitterFactor)).toLong().coerceAtLeast(1)
        val jitteredMillis = (minimumMillis.toDouble() + (cappedMillis - minimumMillis).toDouble() * sample).toLong()
        return Duration.ofMillis(jitteredMillis.coerceIn(minimumMillis, cappedMillis))
    }
}
