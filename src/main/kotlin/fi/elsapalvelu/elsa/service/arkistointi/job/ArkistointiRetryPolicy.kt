package fi.elsapalvelu.elsa.service.arkistointi.job

import java.time.Duration
import kotlin.math.pow

class ArkistointiRetryPolicy {
    /** Total attempts, including the first execution and interrupted executions. */
    var maxAttempts: Int = 5
    var initialDelay: Duration = Duration.ofMinutes(1)
    var maxDelay: Duration = Duration.ofHours(1)
    var multiplier: Double = 2.0

    fun validate() {
        require(maxAttempts > 0) { "Archiving max-attempts must be positive" }
        require(initialDelay >= Duration.ofMillis(1)) { "Archiving initial-delay must be at least 1ms" }
        require(maxDelay >= initialDelay) { "Archiving max-delay must be at least initial-delay" }
        require(multiplier.isFinite() && multiplier >= 1.0) { "Archiving multiplier must be finite and at least 1" }
    }

    fun delay(previousFailures: Int): Duration {
        val millis = initialDelay.toMillis().toDouble() * multiplier.pow(previousFailures.coerceAtLeast(0))
        return Duration.ofMillis(millis.coerceAtMost(maxDelay.toMillis().toDouble()).toLong())
    }
}
