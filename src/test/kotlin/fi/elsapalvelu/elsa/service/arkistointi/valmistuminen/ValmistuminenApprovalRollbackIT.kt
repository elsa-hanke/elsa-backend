package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/** Failures inside the approval transaction roll back the approval, snapshots and queue row. */
class ValmistuminenApprovalRollbackIT : ValmistuminenArchiveItBase() {

    @ParameterizedTest
    @ValueSource(strings = ["scheduler", "manifest"])
    fun `scheduler or deferred manifest failure rolls back the entire approval`(failure: String) {
        val fixture = fixture("EL")
        val triggerBody = if (failure == "scheduler") {
            "RAISE EXCEPTION 'Simulated scheduler insertion failure' USING ERRCODE = '23514';"
        } else {
            """
                INSERT INTO arkistointi_job_asiakirja
                    (job_id, asiakirja_id, asiakirjatyyppi, jarjestys, tiedostonimi, sisaltotyyppi, sha256)
                SELECT job_id, asiakirja_id, asiakirjatyyppi, 99, tiedostonimi, sisaltotyyppi, sha256
                FROM arkistointi_job_asiakirja WHERE job_id = NEW.task_instance::uuid
                ORDER BY id LIMIT 1;
                RETURN NEW;
            """.trimIndent()
        }
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION fail_graduation_schedule() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN
                    $triggerBody
                END; ${'$'}${'$'}
            """.trimIndent())
            jdbc.execute("CREATE TRIGGER fail_graduation_schedule BEFORE INSERT ON scheduled_tasks " +
                "FOR EACH ROW EXECUTE FUNCTION fail_graduation_schedule()")
        }
        val taskCountBefore = allTaskCount()
        try {
            val exception = assertThrows<RuntimeException> { approve(fixture) }
            assertThat(exception).hasStackTraceContaining(if (failure == "scheduler")
                "Simulated scheduler insertion failure" else "Archive job document count does not match the manifest")
            assertRolledBack(fixture)
            assertThat(allTaskCount()).isEqualTo(taskCountBefore)
        } finally {
            transactions.executeWithoutResult {
                jdbc.execute("DROP TRIGGER fail_graduation_schedule ON scheduled_tasks")
                jdbc.execute("DROP FUNCTION fail_graduation_schedule()")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["MISSING", "EMPTY", "INVALID"])
    fun `input document failure rolls back approval snapshots and queue with safe HTTP error`(kind: String) {
        val fixture = fixture("EL")
        val tasksBefore = allTaskCount()
        val code = "ARCHIVE_DOCUMENT_$kind"
        doAnswer { invocation ->
            if (invocation.getArgument<Int>(2) == 1) {
                throw ArkistointiAsiakirjaException(
                    ArkistointiError.create(code, PRIVATE_CERTIFICATE_NAME), ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT
                )
            }
            invocation.callRealMethod()
        }.`when`(snapshots).createReference(any(), any(), any())

        val exception = assertArchiveDocumentHttpFailure(mvc, fixture.id, fixture.approverUserId, 400, code)

        assertThat(exception.error.code).isEqualTo(code)
        assertRolledBack(fixture)
        assertThat(allTaskCount()).isEqualTo(tasksBefore)
        verify(transport, never()).laheta(any(), any())
    }

    @ParameterizedTest
    @ValueSource(strings = ["STORED_DOCUMENT", "PROCESSING_FAILURE"])
    fun `server document errors return safe 500 and roll back approval`(category: String) {
        val fixture = fixture("EL")
        val tasksBefore = allTaskCount()
        if (category == "STORED_DOCUMENT") {
            doAnswer { invocation ->
                (invocation.callRealMethod() as ArkistointiAsiakirjaReference).copy(sha256 = "0".repeat(64))
            }.`when`(snapshots).createReference(any(), any(), any())
        } else {
            doThrow(ArkistointiAsiakirjaException(
                ArkistointiError.create("ARCHIVE_DOCUMENT_PROCESSING_FAILED", PRIVATE_CERTIFICATE_NAME),
                ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE
            ).apply { addSuppressed(IllegalStateException(PRIVATE_CERTIFICATE_NAME)) })
                .`when`(snapshots).createReference(any(), any(), any())
        }
        val code = if (category == "STORED_DOCUMENT") "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH" else "ARCHIVE_DOCUMENT_PROCESSING_FAILED"

        val exception = assertArchiveDocumentHttpFailure(mvc, fixture.id, fixture.approverUserId, 500, code)

        assertThat(exception.category).isEqualTo(ArkistointiAsiakirjaErrorCategory.valueOf(category))
        assertRolledBack(fixture)
        assertThat(allTaskCount()).isEqualTo(tasksBefore)
        verify(transport, never()).laheta(any(), any())
    }
}
