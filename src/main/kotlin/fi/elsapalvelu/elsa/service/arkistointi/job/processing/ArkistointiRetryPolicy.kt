package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiRetryProperties
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import kotlin.math.pow

class ArkistointiRetryPolicy(
    properties: ArkistointiRetryProperties = ArkistointiRetryProperties(),
    private val randomSample: () -> Double = { ThreadLocalRandom.current().nextDouble() }
) {
    // Capture validated settings once; configuration binding must not mutate a running policy.
    init {
        properties.validate()
    }

    val maxAttempts: Int = properties.maxAttempts
    private val initialDelay = properties.initialDelay
    private val maxDelay = properties.maxDelay
    private val multiplier = properties.multiplier
    private val jitterFactor = properties.jitterFactor

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
