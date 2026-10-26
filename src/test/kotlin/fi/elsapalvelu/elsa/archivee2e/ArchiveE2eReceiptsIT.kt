package fi.elsapalvelu.elsa.archivee2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.sql.Connection
import java.security.MessageDigest
import java.util.UUID
import java.util.HexFormat
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.jdbc.datasource.DriverManagerDataSource

class ArchiveE2eReceiptsIT {
    @Test
    fun `test adapters persist actual bytes digest and count repeated calls on non autocommit connections`() {
        val mapper = jacksonObjectMapper()
        val source = DriverManagerDataSource(
            "jdbc:tc:postgresql:16.9:///archiveE2eReceipts?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true", "elsaBackend", ""
        )
        // Production Hikari disables auto-commit. Receipts must commit independently of the processor transaction.
        val dataSource = object : DelegatingDataSource(source) {
            override fun getConnection(): Connection = super.getConnection().apply { autoCommit = false }
        }
        ApplicationContextRunner()
            .withInitializer { it.environment.setActiveProfiles("archive-e2e") }
            .withBean(DataSource::class.java, { dataSource })
            .withBean(com.fasterxml.jackson.databind.ObjectMapper::class.java, { mapper })
            .withUserConfiguration(ArchiveE2eConfiguration::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                val adapters = context.getBeansOfType(ArkistointiAdapter::class.java).values
                assertThat(adapters.map { it.university }).containsExactlyInAnyOrder(
                    YliopistoEnum.HELSINGIN_YLIOPISTO, YliopistoEnum.ITA_SUOMEN_YLIOPISTO
                )
                val adapter = adapters.single { it.university == YliopistoEnum.HELSINGIN_YLIOPISTO }
                assertThat(adapter.supportedCaseTypes).containsExactlyInAnyOrderElementsOf(CaseType.entries)
                val id = UUID.randomUUID()
                val bytes = "test PDF bytes".toByteArray()
                val request = ArkistointiSubmission(
                    id, adapter.university, CaseType.SOPIMUS,
                    """{"caseId":42,"yek":false,"approver":"E2E Approver","secret":"not recorded"}""",
                    listOf(LoadedArkistointiAsiakirja(
                        7L, RecordType.SOPIMUS, 0, "not-recorded.pdf", "application/pdf", "0".repeat(64), bytes
                    ))
                )
                repeat(2) { assertThat(adapter.arkistoi(request)).isEqualTo(ArkistointiResult.Success("e2e:$id")) }
                // A new independent connection sees the committed receipt, including the second invocation.
                val receipt = JdbcTemplate(source).queryForMap("select * from archive_e2e.receipt where job_id = ?", id)
                assertThat(receipt["job_id"]).isEqualTo(id)
                assertThat(receipt["delivery_key"]).isEqualTo(id)
                assertThat(receipt["invocation_count"]).isEqualTo(2)
                val metadata = mapper.readTree(receipt["metadata"].toString())
                assertThat(metadata["caseId"].asLong()).isEqualTo(42L)
                assertThat(metadata.has("secret")).isFalse()
                val document = mapper.readTree(receipt["documents"].toString())[0]
                assertThat(document["size"].asInt()).isEqualTo(bytes.size)
                val expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                assertThat(document["sha256"].asText()).isEqualTo(expectedHash)
                assertThat(document.has("filename")).isFalse()
            }
    }
}
