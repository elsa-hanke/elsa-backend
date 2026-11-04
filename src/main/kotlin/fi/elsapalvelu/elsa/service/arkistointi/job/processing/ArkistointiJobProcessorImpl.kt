package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** Delivery replay safety belongs to the transport; unknown outcomes must not trigger blind retries. */
@Service
class ArkistointiJobProcessorImpl(
    private val state: ArkistointiJobStateService,
    private val loader: ArkistointiAsiakirjaLoader,
    private val adapters: ArkistointiAdapterRegistry
) : ArkistointiJobProcessor {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun process(execution: ArkistointiJobExecution) {
        try {
            processSubmission(execution)
        } catch (exception: ParkArkistointiExecutionException) {
            throw exception
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
        val prepared = state.prepare(execution) ?: return
        val adapter = adapters.find(prepared.university)
            ?: throw ArkistointiProcessingException(
                ArkistointiError.create("ARCHIVE_ADAPTER_MISSING", "Yliopiston arkistointiadapteria ei ole maaritelty"),
                retryable = false
            )
        if (prepared.caseType !in adapter.supportedCaseTypes) {
            throw ArkistointiProcessingException(
                ArkistointiError.create("ARCHIVE_CASE_TYPE_UNSUPPORTED", "Arkistointiadapteri ei tue asiatyyppia"),
                retryable = false
            )
        }
        val submission = ArkistointiSubmission(
            prepared.jobId, prepared.university, prepared.caseType, prepared.payload,
            prepared.asiakirjat.map(loader::load), execution
        )
        // This narrows the stale-send window. Lost active executions are parked: an in-flight send cannot be cancelled by fencing.
        if (!state.checkBeforeSend(execution)) return
        when (val result = adapter.arkistoi(submission)) {
            is ArkistointiResult.Success -> {
                ArkistointiExternalId.validate(result.externalId)
                try {
                    state.succeed(execution, result.externalId)
                } catch (exception: StaleArkistointiExecutionException) {
                    throw exception
                } catch (exception: ParkArkistointiExecutionException) {
                    throw exception
                } catch (_: Exception) {
                    // Acceptance is known, but the commit may have failed or its acknowledgement was lost.
                    // Only a resumable adapter may be invoked again automatically.
                    throw ArkistointiProcessingException(
                        ArkistointiError.create("ARCHIVE_SUCCESS_PERSISTENCE_FAILED",
                            "Arkisto vastaanotti aineiston, mutta tuloksen tallennus epaonnistui; toimitus on tarkistettava"),
                        retryable = adapter.canResumeAfterSuccessPersistenceFailure
                    )
                }
            }
            is ArkistointiResult.RetryableFailure -> throw ArkistointiProcessingException(result.error)
            is ArkistointiResult.PermanentFailure -> throw ArkistointiProcessingException(result.error, retryable = false)
        }
    }
}

internal fun unexpectedArchiveError() = ArkistointiError.create("ARCHIVE_PROCESSING_FAILED", "Arkistoinnin kasittely epaonnistui")
