package fi.elsapalvelu.elsa.config

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobScheduler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.time.Instant
import java.util.Optional
import javax.sql.DataSource

val ARKISTOINTI_JOB_TASK: TaskDescriptor<Unit> =
    TaskDescriptor.of("arkistointi-job", Unit::class.java)

@Configuration
class ArkistointiDbSchedulerConfiguration {

    @Bean
    fun arkistointiJobTask(
        processorProvider: ObjectProvider<ArkistointiJobProcessor>
    ): OneTimeTask<Unit> = Tasks.oneTime(ARKISTOINTI_JOB_TASK)
        .execute { taskInstance, _ ->
            val jobId = taskInstance.id.toLongOrNull()
                ?: throw IllegalArgumentException("Arkistointi-jobin tunniste ei ole numero")
            val processor = processorProvider.getIfAvailable()
                ?: error("ArkistointiJobProcessor is not registered")
            processor.process(jobId)
        }

    @Bean
    fun dbSchedulerCustomizer(dataSource: DataSource): DbSchedulerCustomizer =
        object : DbSchedulerCustomizer {
            override fun dataSource(): Optional<DataSource> =
                Optional.of(AutoCommitDataSource(dataSource))
        }

    private class AutoCommitDataSource(dataSource: DataSource) : DelegatingDataSource(dataSource) {
        override fun getConnection(): Connection =
            super.getConnection().apply { autoCommit = true }

        override fun getConnection(username: String, password: String): Connection =
            super.getConnection(username, password).apply { autoCommit = true }
    }
}

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
    override fun schedule(jobId: Long, executionTime: Instant): Boolean =
        schedulerClient.scheduleIfNotExists(
            ARKISTOINTI_JOB_TASK.instance(jobId.toString()).scheduledTo(executionTime)
        )
}
