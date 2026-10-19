package fi.elsapalvelu.elsa.service.arkistointi.job

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** The adapter must deduplicate uncertain deliveries using submission.key. */
@Service
class ArkistointiJobProcessorImpl(
    private val state: ArkistointiJobStateService,
    adapters: List<ArkistointiAdapter>
) : ArkistointiJobProcessor {
    private val adaptersByUniversity = adapters.associateBy { it.university }.also {
        require(it.size == adapters.size) { "Multiple archive job adapters configured for the same university" }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun process(jobId: Long) {
        try {
            processSubmission(jobId)
        } catch (exception: ArkistointiProcessingException) {
            if (exception.retryable) throw exception
            recordPermanentFailure(jobId, exception.error)
        } catch (exception: ArkistointiAsiakirjaException) {
            // Document errors may contain personal filenames: persist only the code and a fixed description.
            recordPermanentFailure(jobId, ArkistointiError.create(exception.error.code, "Arkistoitavan asiakirjan tarkistus epaonnistui"))
        } catch (_: Exception) {
            // No original exception/cause: scheduler logs must not expose payloads or HTTP response bodies.
            throw ArkistointiProcessingException(unexpectedArchiveError())
        }
    }

    private fun recordPermanentFailure(jobId: Long, error: ArkistointiError) {
        try {
            state.fail(jobId, error, terminal = true)
        } catch (_: Exception) {
            throw ArkistointiProcessingException(unexpectedArchiveError())
        }
    }

    private fun processSubmission(jobId: Long) {
        val submission = state.prepare(jobId) ?: return
        val adapter = adaptersByUniversity[submission.university]
            ?: throw ArkistointiProcessingException(
                ArkistointiError.create("ARCHIVE_ADAPTER_MISSING", "Yliopiston arkistointiadapteria ei ole maaritelty"),
                retryable = false
            )
        when (val result = adapter.arkistoi(submission)) {
            is ArkistointiResult.Success -> state.succeed(jobId, result.externalId)
            is ArkistointiResult.RetryableFailure -> throw ArkistointiProcessingException(result.error)
            is ArkistointiResult.PermanentFailure -> state.fail(jobId, result.error, terminal = true)
        }
    }
}

/** Only safe, adapter-sanitized descriptions are allowed here. Never attach the original cause. */
class ArkistointiProcessingException(
    val error: ArkistointiError,
    val retryable: Boolean = true
) : RuntimeException(error.code)

internal fun unexpectedArchiveError() =
    ArkistointiError.create("ARCHIVE_PROCESSING_FAILED", "Arkistoinnin kasittely epaonnistui")
