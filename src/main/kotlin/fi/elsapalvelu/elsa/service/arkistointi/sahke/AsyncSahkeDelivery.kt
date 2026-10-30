package fi.elsapalvelu.elsa.service.arkistointi.sahke

import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files

/** Owns the async package lifecycle; transport policies and errors belong to the adapter. */
@Component
class AsyncSahkeDelivery(private val builder: AsyncSahkePackageBuilder) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun deliver(
        submission: ArkistointiSubmission,
        adapter: ArkistointiAdapter,
        transport: String,
        invalid: (String) -> ArkistointiResult,
        send: (PreparedSahkePackage) -> ArkistointiResult
    ): ArkistointiResult {
        if (submission.university != adapter.university || submission.caseType !in adapter.supportedCaseTypes) {
            return invalid("${transport}_SUBMISSION_INVALID")
        }
        val directory = Files.createTempDirectory("elsa-${transport.lowercase()}-")
        return try {
            send(builder.build(submission, directory))
        } catch (exception: SahkePackageException) {
            invalid("${transport}_${exception.failure.name}")
        } finally {
            try {
                // Also removes partial output when building or sending fails.
                Files.list(directory).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
                Files.deleteIfExists(directory)
            } catch (_: IOException) {
                // Never log paths or the original exception: filenames may contain personal data.
                log.warn("{} temporary package cleanup failed", transport)
            }
        }
    }
}
