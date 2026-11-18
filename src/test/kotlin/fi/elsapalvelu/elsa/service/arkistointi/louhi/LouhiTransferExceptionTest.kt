package fi.elsapalvelu.elsa.service.arkistointi.louhi

import org.apache.sshd.common.SshConstants
import org.apache.sshd.common.SshException
import org.apache.sshd.sftp.common.SftpConstants
import org.apache.sshd.sftp.common.SftpException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import java.util.stream.Stream

class LouhiTransferExceptionTest {
    @ParameterizedTest
    @MethodSource("diagnosticFailures")
    fun `recognizes structured causes without retaining private diagnostics`(
        source: Exception, expected: LouhiTransferReason
    ) {
        val failure = LouhiTransferException.from(source, LouhiTransferStage.CONNECT)

        assertThat(failure.reason).isEqualTo(expected)
        assertThat(failure.message).contains("stage=CONNECT", "reason=${expected.name}")
        assertSafe(failure)
    }

    @ParameterizedTest
    @EnumSource(LouhiTransferStage::class)
    fun `nested diagnostics do not alter delivery classification`(stage: LouhiTransferStage) {
        val source = IOException(PRIVATE_MESSAGE, SftpException(SftpConstants.SSH_FX_PERMISSION_DENIED, PRIVATE_MESSAGE))
        val failure = LouhiTransferException.from(source, stage)

        assertThat(failure.reason).isEqualTo(LouhiTransferReason.SFTP_REJECTION)
        // Only a top-level SFTP refusal affects the existing delivery classification.
        assertThat(failure.sftpStatus).isNull()
        assertThat(failure.failure).isEqualTo(
            if (stage == LouhiTransferStage.RENAME) LouhiTransferFailure.DELIVERY_UNCERTAIN
            else LouhiTransferFailure.TRANSFER_FAILED
        )
        assertSafe(failure)
    }

    @Test
    fun `rename timeout remains uncertain and explicit refusal retains its status`() {
        val timeout = LouhiTransferException.from(SocketTimeoutException(PRIVATE_MESSAGE), LouhiTransferStage.RENAME)
        assertThat(timeout.reason).isEqualTo(LouhiTransferReason.TIMEOUT)
        assertThat(timeout.failure).isEqualTo(LouhiTransferFailure.DELIVERY_UNCERTAIN)
        val refusal = LouhiTransferException.from(
            SftpException(SftpConstants.SSH_FX_PERMISSION_DENIED, PRIVATE_MESSAGE), LouhiTransferStage.RENAME
        )
        assertThat(refusal.failure).isEqualTo(LouhiTransferFailure.ACCESS_DENIED)
        assertThat(refusal.sftpStatus).isEqualTo(SftpConstants.SSH_FX_PERMISSION_DENIED)
        assertSafe(timeout)
        assertSafe(refusal)
    }

    @Test
    fun `cause cycles and excessive depth terminate safely`() {
        val first = Exception(PRIVATE_MESSAGE)
        val second = Exception(PRIVATE_MESSAGE, first)
        first.initCause(second)
        assertThat(LouhiTransferException.from(first, LouhiTransferStage.CONNECT).reason)
            .isEqualTo(LouhiTransferReason.UNKNOWN)

        var deep: Exception = SocketTimeoutException(PRIVATE_MESSAGE)
        repeat(100) { deep = Exception(PRIVATE_MESSAGE, deep) }
        val failure = LouhiTransferException.from(deep, LouhiTransferStage.CONNECT)
        assertThat(failure.reason).isEqualTo(LouhiTransferReason.UNKNOWN)
        assertSafe(failure)
    }

    private fun assertSafe(failure: LouhiTransferException) {
        assertThat(failure.cause).isNull()
        assertThat(failure.suppressed).isEmpty()
        assertThat(failure.stackTraceToString()).doesNotContain(PRIVATE_MESSAGE, "PrivatePerson", "/secret/", "password")
    }

    companion object {
        private const val PRIVATE_MESSAGE = "PrivatePerson /secret/document.zip password=secret"

        @JvmStatic
        fun diagnosticFailures(): Stream<Arguments> = Stream.of(
            Arguments.of(SocketTimeoutException(PRIVATE_MESSAGE), LouhiTransferReason.TIMEOUT),
            Arguments.of(IOException(PRIVATE_MESSAGE, TimeoutException(PRIVATE_MESSAGE)), LouhiTransferReason.TIMEOUT),
            Arguments.of(
                IOException(PRIVATE_MESSAGE, SshException(SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE, PRIVATE_MESSAGE)),
                LouhiTransferReason.AUTHENTICATION_FAILED
            ),
            Arguments.of(ConnectException(PRIVATE_MESSAGE), LouhiTransferReason.CONNECTION_REFUSED),
            Arguments.of(IOException(PRIVATE_MESSAGE, EOFException(PRIVATE_MESSAGE)), LouhiTransferReason.CONNECTION_LOST),
            Arguments.of(SftpException(SftpConstants.SSH_FX_PERMISSION_DENIED, PRIVATE_MESSAGE), LouhiTransferReason.SFTP_REJECTION),
            Arguments.of(SftpException(SftpConstants.SSH_FX_CONNECTION_LOST, PRIVATE_MESSAGE), LouhiTransferReason.CONNECTION_LOST),
            Arguments.of(IOException(PRIVATE_MESSAGE), LouhiTransferReason.IO_FAILURE),
            Arguments.of(SshException("auth timeout $PRIVATE_MESSAGE"), LouhiTransferReason.UNKNOWN),
            Arguments.of(IllegalStateException(PRIVATE_MESSAGE), LouhiTransferReason.UNKNOWN)
        )
    }
}
