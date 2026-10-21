package fi.elsapalvelu.elsa.config.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiJobScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArkistointiSchedulerArchitectureTest {

    // Exact infrastructure owners, including their Kotlin-generated nested classes, not the whole config package.
    private val infrastructure = listOf(
        ArkistointiDbSchedulerConfiguration::class.java.name,
        "fi.elsapalvelu.elsa.config.arkistointi.ArkistointiDbSchedulerConfigurationKt",
        DbSchedulerArkistointiJobScheduler::class.java.name
    )
    private val schedulerTypes = listOf(Scheduler::class.java.name, SchedulerClient::class.java.name)

    private val rule = noClasses()
        .that(DescribedPredicate.describe<JavaClass>("are outside the approved scheduler infrastructure") { candidate ->
            infrastructure.none { candidate.name == it || candidate.name.startsWith("$it$") }
        })
        .should().dependOnClassesThat(
            DescribedPredicate.describe<JavaClass>("are db-scheduler Scheduler or SchedulerClient (including builders)") { candidate ->
                schedulerTypes.any { candidate.name == it || candidate.name.startsWith("$it$") }
            }
        )
        .because("application code must schedule archive jobs through the transactional ArkistointiJobScheduler")

    @Test
    fun `production code only uses scheduler clients inside approved infrastructure`() {
        val classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("fi.elsapalvelu.elsa")

        // Guard against an accidentally empty scan or an over-broad exclusion.
        assertThat(classes.contain(DbSchedulerArkistointiJobScheduler::class.java)).isTrue()
        rule.check(classes)
    }

    @Test
    fun `rule rejects direct client worker and builder dependencies`() {
        val classes = ClassFileImporter().importClasses(
            DirectClientService::class.java, DirectWorkerService::class.java, DirectBuilderService::class.java
        )

        val result = rule.evaluate(classes)

        assertThat(result.hasViolation()).isTrue()
        assertThat(result.failureReport.details.joinToString("\n"))
            .contains(DirectClientService::class.java.name)
            .contains(DirectWorkerService::class.java.name)
            .contains(DirectBuilderService::class.java.name)
    }

    @Test
    fun `rule allows the transactional application interface`() {
        rule.check(ClassFileImporter().importClasses(TransactionalService::class.java))
    }

    private class DirectClientService(val scheduler: SchedulerClient)
    private class DirectWorkerService(val scheduler: Scheduler)
    private class DirectBuilderService(val builder: SchedulerClient.Builder)
    private class TransactionalService(val scheduler: ArkistointiJobScheduler)
}
