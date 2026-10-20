package fi.elsapalvelu.elsa.service.arkistointi.job

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** The adapter must atomically deduplicate concurrent and uncertain deliveries using submission.key. */
@Service
class ArkistointiJobProcessorImpl(
    private val state: ArkistointiJobStateService,
    adapters: List<ArkistointiAdapter>
) : ArkistointiJobProcessor {
    private val adaptersByUniversity = adapters.associateBy { it.university }.also {
        require(it.size == adapters.size) { "Multiple archive job adapters configured for the same university" }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun process(execution: ArkistointiJobExecution) {
        try {
            processSubmission(execution)
        } catch (exception: StaleArkistointiExecutionException) {
            throw exception
        } catch (exception: ArkistointiProcessingException) {
            // The failure handler owns failure persistence, including permanent outcomes.
            throw exception
        } catch (exception: ArkistointiAsiakirjaException) {
            // Keep a fixed description even for malformed adapter/library descriptions. Never retain a cause chain.
            throw ArkistointiProcessingException(
                ArkistointiError.create(exception.error.code, "Arkistoitavan asiakirjan tarkistus epaonnistui"),
                retryable = exception.category == ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE
            )
        } catch (_: Exception) {
            // No original exception/cause: scheduler logs must not expose payloads or HTTP response bodies.
            throw ArkistointiProcessingException(unexpectedArchiveError())
        }
    }

    private fun processSubmission(execution: ArkistointiJobExecution) {
        val submission = state.prepare(execution) ?: return
        val adapter = adaptersByUniversity[submission.university]
            ?: throw ArkistointiProcessingException(
                ArkistointiError.create("ARCHIVE_ADAPTER_MISSING", "Yliopiston arkistointiadapteria ei ole maaritelty"),
                retryable = false
            )
        when (val result = adapter.arkistoi(submission)) {
            is ArkistointiResult.Success -> {
                ArkistointiExternalId.validate(result.externalId)
                state.succeed(execution, result.externalId)
            }
            is ArkistointiResult.RetryableFailure -> throw ArkistointiProcessingException(result.error)
            is ArkistointiResult.PermanentFailure -> throw ArkistointiProcessingException(result.error, retryable = false)
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
