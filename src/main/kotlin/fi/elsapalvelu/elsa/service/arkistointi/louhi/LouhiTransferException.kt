package fi.elsapalvelu.elsa.service.arkistointi.louhi

import org.apache.sshd.common.SshConstants
import org.apache.sshd.common.SshException
import org.apache.sshd.sftp.common.SftpConstants
import org.apache.sshd.sftp.common.SftpException
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.channels.ClosedChannelException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.TimeoutException

enum class LouhiTransferStage { CONNECT, LIST, WRITE, RENAME }

enum class LouhiTransferReason {
    TIMEOUT, AUTHENTICATION_FAILED, CONNECTION_REFUSED, CONNECTION_LOST, SFTP_REJECTION, IO_FAILURE, UNKNOWN
}

enum class LouhiTransferFailure(val code: String, val retryable: Boolean) {
    TRANSFER_FAILED("LOUHI_TRANSFER_FAILED", true),
    DELIVERY_UNCERTAIN("LOUHI_DELIVERY_UNCERTAIN", false),
    ACCESS_DENIED("LOUHI_ACCESS_DENIED", false),
    // Refusal of this operation, not proof that another execution has not already published the package.
    PATH_NOT_FOUND("LOUHI_REMOTE_PATH_MISSING", false),
    // Classifies a server response; Session.rename itself permits replacing an existing destination.
    TARGET_EXISTS("LOUHI_REMOTE_TARGET_EXISTS", false),
    UNSUPPORTED("LOUHI_OPERATION_UNSUPPORTED", false),
    INVALID_FILENAME("LOUHI_REMOTE_FILENAME_INVALID", false),
    STORAGE_UNAVAILABLE("LOUHI_REMOTE_STORAGE_UNAVAILABLE", true),
    REMOTE_BUSY("LOUHI_REMOTE_BUSY", true)
}

/** Safe transport classification; deliberately retains no SFTP message, filename or cause chain. */
class LouhiTransferException(
    val stage: LouhiTransferStage,
    val failure: LouhiTransferFailure,
    val sftpStatus: Int? = null,
    val reason: LouhiTransferReason = LouhiTransferReason.UNKNOWN
) : RuntimeException("Louhi file transfer failed: ${failure.code} [stage=${stage.name}, reason=${reason.name}, status=$sftpStatus]") {
    /**
     * Marks an unclassified/uncertain rename outcome for this invocation only.
     * False is not evidence that no package exists: an earlier or competing execution may have published it.
     */
    val publicationMayHaveStarted: Boolean
        get() = failure == LouhiTransferFailure.DELIVERY_UNCERTAIN

    companion object {
        fun from(exception: Exception, stage: LouhiTransferStage): LouhiTransferException {
            // Explicit statuses classify the refusal of this invocation, not the delivery's global publication state.
            // Session.rename may delete the target before moving the source (SFTP < v5), or overwrite it (v5+).
            // Generic FAILURE cannot identify which internal step failed and remains uncertain during rename.
            val status = (exception as? SftpException)?.status
            val rejection = when (status) {
                SftpConstants.SSH_FX_PERMISSION_DENIED, SftpConstants.SSH_FX_WRITE_PROTECT -> LouhiTransferFailure.ACCESS_DENIED
                SftpConstants.SSH_FX_NO_SUCH_FILE, SftpConstants.SSH_FX_NO_SUCH_PATH -> LouhiTransferFailure.PATH_NOT_FOUND
                SftpConstants.SSH_FX_FILE_ALREADY_EXISTS -> LouhiTransferFailure.TARGET_EXISTS
                SftpConstants.SSH_FX_OP_UNSUPPORTED -> LouhiTransferFailure.UNSUPPORTED
                SftpConstants.SSH_FX_INVALID_FILENAME -> LouhiTransferFailure.INVALID_FILENAME
                SftpConstants.SSH_FX_NO_SPACE_ON_FILESYSTEM, SftpConstants.SSH_FX_QUOTA_EXCEEDED ->
                    LouhiTransferFailure.STORAGE_UNAVAILABLE
                SftpConstants.SSH_FX_LOCK_CONFLICT, SftpConstants.SSH_FX_BYTE_RANGE_LOCK_CONFLICT,
                SftpConstants.SSH_FX_BYTE_RANGE_LOCK_REFUSED -> LouhiTransferFailure.REMOTE_BUSY
                else -> null
            }
            val failure = rejection ?: if (stage == LouhiTransferStage.RENAME) {
                LouhiTransferFailure.DELIVERY_UNCERTAIN
            } else {
                LouhiTransferFailure.TRANSFER_FAILED
            }
            return LouhiTransferException(stage, failure, status, diagnosticReason(exception))
        }

        private const val MAX_CAUSE_DEPTH = 16

        private fun diagnosticReason(exception: Exception): LouhiTransferReason {
            val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
            val causes = mutableListOf<Throwable>()
            var current: Throwable? = exception
            while (current != null && causes.size < MAX_CAUSE_DEPTH && seen.add(current)) {
                causes.add(current)
                current = current.cause
            }
            // Inspect specific structured causes before a generic IOException wrapper. Never inspect messages.
            for (cause in causes) {
                structuredReason(cause)?.let { return it }
            }
            return if (causes.any { it is IOException && it !is SshException }) LouhiTransferReason.IO_FAILURE
                else LouhiTransferReason.UNKNOWN
        }

        private fun structuredReason(cause: Throwable): LouhiTransferReason? = when {
            cause is SocketTimeoutException || cause is TimeoutException -> LouhiTransferReason.TIMEOUT
            cause is SshException &&
                cause.disconnectCode == SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE ->
                LouhiTransferReason.AUTHENTICATION_FAILED
            cause is ConnectException -> LouhiTransferReason.CONNECTION_REFUSED
            cause is EOFException || cause is ClosedChannelException || cause is SocketException ->
                LouhiTransferReason.CONNECTION_LOST
            cause is SftpException -> when (cause.status) {
                SftpConstants.SSH_FX_CONNECTION_LOST, SftpConstants.SSH_FX_NO_CONNECTION -> LouhiTransferReason.CONNECTION_LOST
                else -> LouhiTransferReason.SFTP_REJECTION
            }
            else -> null
        }
    }
}
