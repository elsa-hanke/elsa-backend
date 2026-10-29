package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkePackageBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePackageException
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
class SiiloAsyncArkistointiAdapter(
    private val packageBuilder: AsyncSahkePackageBuilder,
    private val siilo: HelsinkiSiiloService
) : ArkistointiAdapter {
    private val log = LoggerFactory.getLogger(javaClass)
    override val university = YliopistoEnum.HELSINGIN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO, CaseType.SOPIMUS)

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        if (pyynto.university != university || pyynto.caseType !in supportedCaseTypes) {
            return failure("SIILO_SUBMISSION_INVALID", false)
        }
        val directory = Files.createTempDirectory("elsa-siilo-")
        return try {
            val archive = packageBuilder.build(pyynto, directory)
            siilo.lahetaAsync(archive.path.toString(), pyynto.caseType)
            ArkistointiResult.Success(null)
        } catch (exception: SahkePackageException) {
            failure("SIILO_${exception.failure.name}", false)
        } catch (exception: SiiloTransferException) {
            failure(exception.code, exception.retryable, exception.httpStatus, exception.category)
        } finally {
            try {
                Files.list(directory).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
                Files.deleteIfExists(directory)
            } catch (_: java.io.IOException) {
                log.warn("Siilo temporary package cleanup failed")
            }
        }
    }

    private fun failure(code: String, retryable: Boolean, status: Int? = null, category: String? = null): ArkistointiResult {
        val error = ArkistointiError.create(code, "Siilo-toimitus epaonnistui [status=$status, category=$category]")
        return if (retryable) ArkistointiResult.RetryableFailure(error) else ArkistointiResult.PermanentFailure(error)
    }
}
