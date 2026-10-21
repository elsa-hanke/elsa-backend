package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.CompletionHandler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.StaleArkistointiExecutionException
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobFailureHandler
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiRetryPolicy
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiConfigurationProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.time.Instant
import java.util.Optional
import java.util.UUID
import javax.sql.DataSource

val ARKISTOINTI_JOB_TASK: TaskDescriptor<Unit> =
    TaskDescriptor.of("arkistointi-job", Unit::class.java)

@Configuration
class ArkistointiDbSchedulerConfiguration {

    @Bean
    fun arkistointiRetryPolicy(properties: ApplicationProperties): ArkistointiRetryPolicy =
        ArkistointiRetryPolicy(properties.getArkistointi().retry)

    @Bean
    fun arkistointiJobTask(
        processorProvider: ObjectProvider<ArkistointiJobProcessor>,
        failureHandler: ArkistointiJobFailureHandler,
        adapters: ArkistointiAdapterRegistry,
        configuration: ArkistointiConfigurationProvider,
        environment: Environment
    ): OneTimeTask<Unit> {
        // Auto-configuration collects Task beans before constructing/starting the worker,
        // including when delay-startup-until-context-ready=false. Producers may have no worker.
        if (environment.getProperty("db-scheduler.enabled", Boolean::class.java, true)) {
            adapters.validateWorkerConfiguration(configuration)
        }
        return object : OneTimeTask<Unit>(
            ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, failureHandler,
            { completed, operations -> failureHandler.onFailure(completed, operations) }
        ) {
            override fun executeOnce(taskInstance: TaskInstance<Unit>, executionContext: ExecutionContext) {
                val execution = ArkistointiJobExecution(taskInstance.taskName, taskInstance.id, executionContext.execution.version)
                val processor = processorProvider.getIfAvailable()
                    ?: error("ArkistointiJobProcessor is not registered")
                processor.process(execution)
            }

            override fun execute(taskInstance: TaskInstance<Unit>, executionContext: ExecutionContext): CompletionHandler<Unit> =
                try {
                    super.execute(taskInstance, executionContext)
                } catch (_: StaleArkistointiExecutionException) {
                    // Returning normally from executeOnce would acknowledge the replaced execution.
                    // Do not touch its scheduler row; normal completions still use version-checked removal.
                    CompletionHandler { _, _ -> }
                }
        }
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
    override fun schedule(jobId: UUID, executionTime: Instant): Boolean =
        schedulerClient.scheduleIfNotExists(
            ARKISTOINTI_JOB_TASK.instance(jobId.toString()).scheduledTo(executionTime)
        )
}
