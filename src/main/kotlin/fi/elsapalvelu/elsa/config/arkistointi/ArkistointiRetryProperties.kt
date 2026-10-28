package fi.elsapalvelu.elsa.config.arkistointi

import java.time.Duration

class ArkistointiRetryProperties {
    var maxAttempts: Int = 25
    var initialDelay: Duration = Duration.ofMinutes(1)
    var maxDelay: Duration = Duration.ofHours(4)
    var multiplier: Double = 2.0
    var jitterFactor: Double = 0.1
    var alertAfterAttempts: Int = 3

    fun validate() {
        require(maxAttempts > 0) { "Archiving max-attempts must be positive" }
        require(alertAfterAttempts > 0) { "Archiving alert-after-attempts must be positive" }
        require(initialDelay >= Duration.ofMillis(1)) { "Archiving initial-delay must be at least 1ms" }
        require(maxDelay >= initialDelay) { "Archiving max-delay must be at least initial-delay" }
        require(multiplier.isFinite() && multiplier >= 1.0) { "Archiving multiplier must be finite and at least 1" }
        require(jitterFactor.isFinite() && jitterFactor >= 0.0 && jitterFactor < 1.0) {
            "Archiving jitter-factor must be finite, at least 0 and less than 1"
        }
    }
}
