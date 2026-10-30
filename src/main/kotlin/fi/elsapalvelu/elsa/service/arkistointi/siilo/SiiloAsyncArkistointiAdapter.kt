package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkeDelivery
import fi.elsapalvelu.elsa.service.arkistointi.sahke.PreparedSahkePackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!e2e-archive")
class SiiloAsyncArkistointiAdapter(
    private val delivery: AsyncSahkeDelivery,
    private val siilo: HelsinkiSiiloService
) : ArkistointiAdapter {
    override val university = YliopistoEnum.HELSINGIN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO, CaseType.SOPIMUS)

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult =
        delivery.deliver(pyynto, this, "SIILO", { failure(it, false) }) {
            send(it, pyynto.caseType)
        }

    private fun send(archive: PreparedSahkePackage, caseType: CaseType): ArkistointiResult =
        try {
            siilo.lahetaAsync(archive.path.toString(), caseType)
            ArkistointiResult.Success(null)
        } catch (exception: SiiloTransferException) {
            failure(exception.code, exception.retryable, exception.httpStatus, exception.category)
        }

    private fun failure(code: String, retryable: Boolean, status: Int? = null, category: String? = null): ArkistointiResult {
        val error = ArkistointiError.create(code, "Siilo-toimitus epaonnistui [status=$status, category=$category]")
        return if (retryable) ArkistointiResult.RetryableFailure(error) else ArkistointiResult.PermanentFailure(error)
    }
}
