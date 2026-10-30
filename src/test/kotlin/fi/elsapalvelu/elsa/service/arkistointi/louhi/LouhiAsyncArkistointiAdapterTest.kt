package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkeDelivery
import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkePackageBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePackageException
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePackageFailure
import fi.elsapalvelu.elsa.service.arkistointi.sahke.PreparedSahkePackage

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import java.io.IOException
import java.util.UUID

class LouhiAsyncArkistointiAdapterTest {
    private val builder = mock<AsyncSahkePackageBuilder>()
    private val louhi = mock<TampereLouhiService>()
    private val adapter = LouhiAsyncArkistointiAdapter(AsyncSahkeDelivery(builder), louhi)
    private val submission = ArkistointiSubmission(UUID.randomUUID(), YliopistoEnum.TAMPEREEN_YLIOPISTO,
        CaseType.VALMISTUMINEN, "{}", emptyList())

    @ParameterizedTest
    @ValueSource(strings = ["success", "retryable", "uncertain"])
    fun `calls existing Louhi sender and cleans temporary files for every outcome`(outcome: String) {
        val paths = mutableListOf<Path>()
        doAnswer { call ->
            val path = call.getArgument<Path>(1).resolve("valmistuminen_matti_meikäläinen.zip")
            paths.add(path)
            Files.writeString(path, "package")
            PreparedSahkePackage(path, true)
        }.whenever(builder).build(eq(submission), any())
        doAnswer { call ->
            val path = Path.of(call.getArgument<String>(0))
            assertThat(path).exists()
            assertThat(path.fileName.toString()).isEqualTo("valmistuminen_matti_meikäläinen.zip")
            if (outcome != "success") throw LouhiTransferException(
                if (outcome == "uncertain") LouhiTransferStage.RENAME else LouhiTransferStage.WRITE,
                if (outcome == "uncertain") LouhiTransferFailure.DELIVERY_UNCERTAIN else LouhiTransferFailure.TRANSFER_FAILED
            )
            Files.delete(path)
            Unit
        }.whenever(louhi).laheta(any(), eq(true), eq(LouhiExecutionPolicy.ASYNC))

        repeat(2) {
            when (val result = adapter.arkistoi(submission)) {
                is ArkistointiResult.Success -> assertThat(outcome).isEqualTo("success")
                is ArkistointiResult.RetryableFailure -> assertThat(outcome).isEqualTo("retryable")
                is ArkistointiResult.PermanentFailure -> {
                    assertThat(outcome).isEqualTo("uncertain")
                    assertThat(result.error.code).isEqualTo("LOUHI_DELIVERY_UNCERTAIN")
                }
            }
        }
        assertThat(paths.map { it.parent }.distinct()).hasSize(2)
        paths.forEach {
            assertThat(it).doesNotExist()
            assertThat(it.parent).doesNotExist()
        }
    }

    @Test
    fun `failed package creation removes partial ZIP and reports safe permanent error`() {
        var directory: Path? = null
        doAnswer { call ->
            directory = call.getArgument(1)
            Files.writeString(directory!!.resolve("partial.zip"), "partial")
            throw SahkePackageException(SahkePackageFailure.PAYLOAD_INVALID)
        }.whenever(builder).build(eq(submission), any())

        val result = adapter.arkistoi(submission) as ArkistointiResult.PermanentFailure

        assertThat(result.error.code).isEqualTo("LOUHI_PAYLOAD_INVALID")
        assertThat(result.error.description).doesNotContain("private", "filename", "personal")
        assertThat(directory!!).doesNotExist()
        verifyNoInteractions(louhi)
    }

    @ParameterizedTest
    @EnumSource(SahkePackageFailure::class)
    fun `known package failure is permanent and prevents sending`(failure: SahkePackageFailure) {
        whenever(builder.build(eq(submission), any())).thenThrow(SahkePackageException(failure))

        val result = adapter.arkistoi(submission) as ArkistointiResult.PermanentFailure

        assertThat(result.error.code).isEqualTo("LOUHI_${failure.name}")
        verifyNoInteractions(louhi)
    }

    @ParameterizedTest
    @ValueSource(strings = ["io", "state", "argument"])
    fun `unknown package failures propagate to the processors bounded retry`(kind: String) {
        val failure = when (kind) {
            "io" -> IOException("private disk details")
            "state" -> IllegalStateException("private state details")
            else -> IllegalArgumentException("unknown library failure")
        }
        var directory: Path? = null
        doAnswer { call ->
            directory = call.getArgument(1)
            Files.writeString(directory!!.resolve("partial.zip"), "partial")
            throw failure
        }.whenever(builder).build(eq(submission), any())

        assertThatThrownBy { adapter.arkistoi(submission) }.isSameAs(failure)
        assertThat(directory!!).doesNotExist()
        verifyNoInteractions(louhi)
    }

    @ParameterizedTest
    @EnumSource(LouhiTransferFailure::class)
    fun `transport classification controls retry and exposes only safe diagnostics`(failure: LouhiTransferFailure) {
        var path: Path? = null
        doAnswer { call ->
            path = call.getArgument<Path>(1).resolve("private-person.zip")
            Files.writeString(path!!, "package")
            PreparedSahkePackage(path, false)
        }.whenever(builder).build(eq(submission), any())
        whenever(louhi.laheta(any(), eq(false), eq(LouhiExecutionPolicy.ASYNC)))
            .thenThrow(LouhiTransferException(LouhiTransferStage.RENAME, failure, 3, LouhiTransferReason.SFTP_REJECTION))

        val result = adapter.arkistoi(submission)
        val error = if (failure.retryable) {
            assertThat(result).isInstanceOf(ArkistointiResult.RetryableFailure::class.java)
            (result as ArkistointiResult.RetryableFailure).error
        } else {
            assertThat(result).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
            (result as ArkistointiResult.PermanentFailure).error
        }
        assertThat(error.code).isEqualTo(failure.code)
        assertThat(error.description).contains("stage=RENAME", "reason=SFTP_REJECTION", "status=3")
            .doesNotContain("private", "person", ".zip")
        assertThat(path!!).doesNotExist()
        assertThat(path.parent).doesNotExist()
    }

    @Test
    fun `does not add SOPIMUS or another university`() {
        assertThat(adapter.supportedCaseTypes).containsExactlyInAnyOrder(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)
        assertThat(adapter.arkistoi(submission.copy(caseType = CaseType.SOPIMUS)))
            .isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
        assertThat(adapter.arkistoi(submission.copy(university = YliopistoEnum.HELSINGIN_YLIOPISTO)))
            .isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
        verifyNoInteractions(builder, louhi)
    }
}
