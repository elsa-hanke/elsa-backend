package fi.elsapalvelu.elsa.service.arkistointi.support

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.test.context.bean.override.convention.TestBean
import java.util.concurrent.CopyOnWriteArrayList

class ArchiveProcessingTestAdapter : ArkistointiAdapter {
    override val university = YliopistoEnum.TURUN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN)
    val submissions = CopyOnWriteArrayList<ArkistointiSubmission>()
    @Volatile var behavior: (ArkistointiSubmission) -> ArkistointiResult = { ArkistointiResult.Success(null) }

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) { "Network call inside transaction" }
        submissions.add(pyynto)
        return behavior(pyynto)
    }
}

/** Shared bean replacement for processor, loader and scheduler recovery integration tests. */
open class ArchiveProcessingTestSupport {
    @TestBean(name = "turkuDynastyArkistointiAdapter", methodName = "createTurkuAdapter", enforceOverride = true)
    private lateinit var testArchiveAdapter: ArkistointiAdapter
    protected val adapter: ArchiveProcessingTestAdapter get() = testArchiveAdapter as ArchiveProcessingTestAdapter

    companion object {
        @JvmStatic
        fun createTurkuAdapter(): ArkistointiAdapter = ArchiveProcessingTestAdapter()
    }
}
