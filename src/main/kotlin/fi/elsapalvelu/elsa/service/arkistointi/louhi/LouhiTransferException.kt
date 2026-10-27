package fi.elsapalvelu.elsa.service.arkistointi.louhi

import org.apache.sshd.sftp.common.SftpConstants
import org.apache.sshd.sftp.common.SftpException

enum class LouhiTransferStage { CONNECT, LIST, WRITE, RENAME }

enum class LouhiTransferFailure(val code: String, val retryable: Boolean) {
    TRANSFER_FAILED("LOUHI_TRANSFER_FAILED", true),
    DELIVERY_UNCERTAIN("LOUHI_DELIVERY_UNCERTAIN", false),
    ACCESS_DENIED("LOUHI_ACCESS_DENIED", false),
    PATH_NOT_FOUND("LOUHI_REMOTE_PATH_MISSING", false),
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
    val sftpStatus: Int? = null
) : RuntimeException("Louhi file transfer failed: ${failure.code} [stage=${stage.name}, status=$sftpStatus]") {
    val publicationMayHaveStarted: Boolean
        get() = failure == LouhiTransferFailure.DELIVERY_UNCERTAIN

    companion object {
        fun from(exception: Exception, stage: LouhiTransferStage): LouhiTransferException {
            // Only these explicit refusal statuses establish that publication did not complete.
            // Generic FAILURE, malformed replies and connection statuses remain uncertain during rename.
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
            return LouhiTransferException(stage, failure, status)
        }
    }
}
