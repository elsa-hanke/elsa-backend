package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class DbSchedulerArkistointiJobScheduler(
    dataSource: DataSource,
    arkistointiJobTask: OneTimeTask<Unit>
) : ArkistointiJobScheduler {

    private val schedulerClient = SchedulerClient.Builder.create(
        TransactionAwareDataSourceProxy(dataSource),
        arkistointiJobTask
    ).build()

    @Transactional(propagation = Propagation.MANDATORY)
    override fun schedule(jobId: UUID, executionTime: Instant): Boolean =
        schedulerClient.scheduleIfNotExists(
            ARKISTOINTI_JOB_TASK.instance(jobId.toString()).scheduledTo(executionTime)
        )
}
