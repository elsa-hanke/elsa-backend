package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessor
import java.sql.Connection
import java.util.Optional
import java.util.UUID
import javax.sql.DataSource
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DelegatingDataSource

val ARKISTOINTI_JOB_TASK: TaskDescriptor<Unit> =
    TaskDescriptor.of("arkistointi-job", Unit::class.java)

@Configuration
class ArkistointiDbSchedulerConfiguration {

    @Bean
    fun arkistointiJobTask(
        processorProvider: ObjectProvider<ArkistointiJobProcessor>
    ): OneTimeTask<Unit> = Tasks.oneTime(ARKISTOINTI_JOB_TASK)
        .execute { taskInstance, _ ->
            val jobId = try {
                UUID.fromString(taskInstance.id).also {
                    require(it.toString().equals(taskInstance.id, ignoreCase = true))
                }
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Arkistointi-jobin tunniste ei ole UUID")
            }
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
