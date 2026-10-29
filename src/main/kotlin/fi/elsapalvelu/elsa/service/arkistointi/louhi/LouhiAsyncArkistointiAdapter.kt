package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkePackageBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePackageException

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.nio.file.Files
import org.springframework.web.server.ResponseStatusException

@Component
@Profile("!e2e-archive | prod | replica")
class LouhiAsyncArkistointiAdapter(
    private val packageBuilder: AsyncSahkePackageBuilder,
    private val louhi: TampereLouhiService
) : ArkistointiAdapter {
    private val log = LoggerFactory.getLogger(javaClass)
    override val university = YliopistoEnum.TAMPEREEN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        if (pyynto.university != university || pyynto.caseType !in supportedCaseTypes) {
            return invalid("LOUHI_SUBMISSION_INVALID")
        }
        val directory = Files.createTempDirectory("elsa-louhi-")
        return try {
            val archive = packageBuilder.build(pyynto, directory)
            louhi.laheta(archive.path.toString(), archive.yek, LouhiExecutionPolicy.ASYNC)
            ArkistointiResult.Success(null)
        } catch (exception: LouhiTransferException) {
            val failure = exception.failure
            val error = ArkistointiError.create(failure.code,
                "Louhi-siirto epaonnistui [stage=${exception.stage.name}, reason=${exception.reason.name}, status=${exception.sftpStatus}]")
            if (failure.retryable) ArkistointiResult.RetryableFailure(error)
            else ArkistointiResult.PermanentFailure(error)
        } catch (_: ResponseStatusException) {
            retryable()
        } catch (exception: SahkePackageException) {
            invalid("LOUHI_${exception.failure.name}")
        } finally {
            try {
                // laheta owns file deletion; also remove partial output if package creation failed.
                Files.list(directory).use { files -> files.forEach { Files.deleteIfExists(it) } }
                Files.deleteIfExists(directory)
            } catch (_: java.io.IOException) {
                log.warn("Louhi temporary package cleanup failed")
            }
        }
    }

    private fun invalid(code: String) = ArkistointiResult.PermanentFailure(
        ArkistointiError.create(code, "Louhi-toimituksen tiedot tai vastaanoton asetukset on tarkistettava")
    )

    private fun retryable() = ArkistointiResult.RetryableFailure(
        ArkistointiError.create("LOUHI_TRANSFER_FAILED", "Louhi-siirto ei valmistunut ennen julkaisua")
    )
}
