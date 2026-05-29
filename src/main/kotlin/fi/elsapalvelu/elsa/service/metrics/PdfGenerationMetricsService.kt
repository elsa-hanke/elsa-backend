package fi.elsapalvelu.elsa.service.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy(false)
class PdfGenerationMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {

    companion object {
        const val OP_LUO_PDF = "luoPdf"
        const val OP_YHDISTA_ASIAKIRJAT = "yhdistaAsiakirjat"
        const val OP_YHDISTA_PDF = "yhdistaPdf"
    }

    private val activePdfOperations = atomicGauge(name = "pdf.generation.active", description = "Number of PDF generation / merge operations currently executing")

    private val requestsLuoPdf = counter("pdf.generation.requests.total", "Total completed PDF generation calls", "operation", OP_LUO_PDF)
    private val requestsYhdistaAsiakirjat = counter("pdf.generation.requests.total", "Total completed PDF generation calls", "operation", OP_YHDISTA_ASIAKIRJAT)
    private val requestsYhdistaPdf = counter("pdf.generation.requests.total", "Total completed PDF generation calls", "operation", OP_YHDISTA_PDF)

    private val errorsLuoPdf = counter("pdf.generation.errors.total", "Total failed PDF generation calls", "operation", OP_LUO_PDF)
    private val errorsYhdistaAsiakirjat = counter("pdf.generation.errors.total", "Total failed PDF generation calls", "operation", OP_YHDISTA_ASIAKIRJAT)
    private val errorsYhdistaPdf = counter("pdf.generation.errors.total", "Total failed PDF generation calls", "operation", OP_YHDISTA_PDF)

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

