package fi.elsapalvelu.elsa.archivee2e

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.security.MessageDigest
import java.util.HexFormat
import javax.sql.DataSource
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/** Not included in bootJar or Jib images. No external archive clients are called. */
@Configuration(proxyBeanMethods = false)
@Profile("archive-e2e & !prod & !replica")
class ArchiveE2eConfiguration {
    @Bean
    @DependsOnDatabaseInitialization
    fun archiveE2eReceipts(dataSource: DataSource, mapper: ObjectMapper): ArchiveE2eReceipts =
        ArchiveE2eReceipts(JdbcTemplate(dataSource), mapper).also { it.initialize() }

    @Bean
    fun archiveE2eHelsinkiAdapter(receipts: ArchiveE2eReceipts): ArkistointiAdapter =
        ArchiveE2eAdapter(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.entries.toSet(), receipts)

    @Bean
    fun archiveE2eUefAdapter(receipts: ArchiveE2eReceipts): ArkistointiAdapter =
        ArchiveE2eAdapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO, setOf(CaseType.VALMISTUMINEN), receipts)
}

class ArchiveE2eAdapter(
    override val university: YliopistoEnum,
    override val supportedCaseTypes: Set<CaseType>,
    private val receipts: ArchiveE2eReceipts
) : ArkistointiAdapter {
    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
        receipts.record(pyynto)
        return ArkistointiResult.Success("e2e:${pyynto.jobId}")
    }
}

class ArchiveE2eReceipts(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    private val transactions = TransactionTemplate(DataSourceTransactionManager(requireNotNull(jdbc.dataSource)))

    fun initialize() {
        transactions.executeWithoutResult {
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS archive_e2e")
            jdbc.execute(
                """CREATE TABLE IF NOT EXISTS archive_e2e.receipt (
                    job_id uuid PRIMARY KEY, delivery_key uuid NOT NULL, university text NOT NULL,
                    case_type text NOT NULL, metadata jsonb NOT NULL, documents jsonb NOT NULL,
                    invocation_count integer NOT NULL DEFAULT 1
                )"""
            )
        }
    }

    fun record(submission: ArkistointiSubmission) {
        val payload = mapper.readTree(submission.payload)
        val metadata = mapper.createObjectNode()
        listOf(
            "caseId", "valmistumispyyntoId", "opintooikeusId", "university", "yek", "specialtyId",
            "reviewer", "approver", "reviewDate", "approvalDate", "koejaksoHyvaksytty"
        ).forEach { field -> payload.get(field)?.let { metadata.set<com.fasterxml.jackson.databind.JsonNode>(field, it) } }
        val documents = submission.asiakirjat.map { document ->
            mapOf(
                "id" to document.asiakirjaId, "type" to document.asiakirjatyyppi.name,
                "order" to document.jarjestysnumero, "size" to document.sisalto.size,
                "contentType" to document.contentType,
                // Hash the actual bytes received at the delivery boundary, independently of the stored checksum.
                "sha256" to HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(document.sisalto))
            )
        }
        transactions.executeWithoutResult {
            jdbc.update(
                """INSERT INTO archive_e2e.receipt(job_id, delivery_key, university, case_type, metadata, documents)
                    VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb)
                    ON CONFLICT (job_id) DO UPDATE SET invocation_count = archive_e2e.receipt.invocation_count + 1""",
                submission.jobId, submission.key, submission.university.name, submission.caseType.name,
                mapper.writeValueAsString(metadata), mapper.writeValueAsString(documents)
            )
        }
    }
}
