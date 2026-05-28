package fi.elsapalvelu.elsa.service.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Tracks PDF-generation workload in real time.
 *
 * Gauges
 * ──────
 * pdf.generation.active          – number of PDF operations currently executing
 *                                  (luoPdf + yhdistaAsiakirjat + yhdistaPdf combined).
 *                                  A sustained non-zero value during off-peak hours is
 *                                  evidence that PDF rendering ties up JVM threads and
 *                                  motivates moving generation to an external service.
 *
 * Counters
 * ────────
 * pdf.generation.requests.total  – lifetime count of completed operations (tag: operation).
 *                                  Use the rate over a rolling window in Grafana to derive
 *                                  daily/hourly volume per operation type.
 * pdf.generation.errors.total    – lifetime count of failed operations (tag: operation).
 */
@Service
@Lazy(false)
class PdfGenerationMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {

    companion object {
        const val OP_LUO_PDF = "luoPdf"
        const val OP_YHDISTA_ASIAKIRJAT = "yhdistaAsiakirjat"
        const val OP_YHDISTA_PDF = "yhdistaPdf"
    }

    /** Number of PDF operations currently executing across all threads. */
    val activePdfOperations = atomicGauge(
        name = "pdf.generation.active",
        description = "Number of PDF generation / merge operations currently executing in the JVM"
    )

    private val requestsLuoPdf = counter(
        "pdf.generation.requests.total",
        "Total completed PDF generation calls",
        "operation", OP_LUO_PDF
    )

    private val requestsYhdistaAsiakirjat = counter(
        "pdf.generation.requests.total",
        "Total completed PDF generation calls",
        "operation", OP_YHDISTA_ASIAKIRJAT
    )

    private val requestsYhdistaPdf = counter(
        "pdf.generation.requests.total",
        "Total completed PDF generation calls",
        "operation", OP_YHDISTA_PDF
    )

    private val errorsLuoPdf = counter(
        "pdf.generation.errors.total",
        "Total failed PDF generation calls",
        "operation", OP_LUO_PDF
    )

    private val errorsYhdistaAsiakirjat = counter(
        "pdf.generation.errors.total",
        "Total failed PDF generation calls",
        "operation", OP_YHDISTA_ASIAKIRJAT
    )

    private val errorsYhdistaPdf = counter(
        "pdf.generation.errors.total",
        "Total failed PDF generation calls",
        "operation", OP_YHDISTA_PDF
    )

    /**
     * Executes [block] while keeping the active-gauge incremented.
     * Decrements the gauge and increments the appropriate counter when [block] finishes
     * (whether normally or via exception).
     */
    fun <T> trackOperation(operation: String, block: () -> T): T {
        activePdfOperations.incrementAndGet()
        return try {
            val result = block()
            requestsCounter(operation).increment()
            result
        } catch (ex: Exception) {
            errorsCounter(operation).increment()
            throw ex
        } finally {
            activePdfOperations.updateAndGet { maxOf(0, it - 1) }
        }
    }

    private fun requestsCounter(operation: String) = when (operation) {
        OP_LUO_PDF -> requestsLuoPdf
        OP_YHDISTA_ASIAKIRJAT -> requestsYhdistaAsiakirjat
        else -> requestsYhdistaPdf
    }

    private fun errorsCounter(operation: String) = when (operation) {
        OP_LUO_PDF -> errorsLuoPdf
        OP_YHDISTA_ASIAKIRJAT -> errorsYhdistaAsiakirjat
        else -> errorsYhdistaPdf
    }
}

