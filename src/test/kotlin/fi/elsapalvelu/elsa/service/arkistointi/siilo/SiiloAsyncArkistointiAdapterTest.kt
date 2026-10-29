package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkePackageBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.PreparedSahkePackage
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePackageException
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePackageFailure
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class SiiloAsyncArkistointiAdapterTest {
    private val builder = mock<AsyncSahkePackageBuilder>()
    private val siilo = mock<HelsinkiSiiloService>()
    private val adapter = SiiloAsyncArkistointiAdapter(builder, siilo)
    private val submission = ArkistointiSubmission(UUID.randomUUID(), YliopistoEnum.HELSINGIN_YLIOPISTO,
        CaseType.KOEJAKSO, "{}", emptyList())

    @ParameterizedTest
    @ValueSource(strings = ["success", "retryable", "permanent"])
    fun `each delivery owns and cleans its directory also on transfer failure`(outcome: String) {
        val directories = mutableListOf<Path>()
        doAnswer { call ->
            val directory = call.getArgument<Path>(1).also(directories::add)
            val path = directory.resolve("assessment.zip")
            Files.writeString(path, "saved archive")
            PreparedSahkePackage(path, false)
        }.whenever(builder).build(eq(submission), any())
        doAnswer { call ->
            assertThat(Path.of(call.getArgument<String>(0))).exists()
            if (outcome != "success") throw SiiloTransferException("SIILO_HTTP_REJECTED", outcome == "retryable", 503)
            Unit
        }.whenever(siilo).lahetaAsync(any(), eq(CaseType.KOEJAKSO))
        repeat(2) {
            val result = adapter.arkistoi(submission)
            when (outcome) {
                "success" -> assertThat(result).isInstanceOf(ArkistointiResult.Success::class.java)
                "retryable" -> assertThat(result).isInstanceOf(ArkistointiResult.RetryableFailure::class.java)
                else -> assertThat(result).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
            }
        }
        assertThat(directories.distinct()).hasSize(2)
        directories.forEach { assertThat(it).doesNotExist() }
    }

    @Test
    fun `invalid saved package is permanent and never sent`() {
        whenever(builder.build(eq(submission), any())).thenThrow(SahkePackageException(SahkePackageFailure.PAYLOAD_INVALID))
        val result = adapter.arkistoi(submission) as ArkistointiResult.PermanentFailure
        assertThat(result.error.code).isEqualTo("SIILO_PAYLOAD_INVALID")
        verifyNoInteractions(siilo)
    }

    @Test
    fun `foreign university is rejected before building or sending`() {
        assertThat(adapter.arkistoi(submission.copy(university = YliopistoEnum.TAMPEREEN_YLIOPISTO)))
            .isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
        verifyNoInteractions(builder, siilo)
    }
}
