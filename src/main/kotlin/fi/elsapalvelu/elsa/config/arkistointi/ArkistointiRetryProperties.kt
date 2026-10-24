package fi.elsapalvelu.elsa.config.arkistointi

import java.time.Duration

/** Bound under application.arkistointi.retry; contains settings, not scheduling logic. */
class ArkistointiRetryProperties {
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
}
