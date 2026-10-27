package fi.elsapalvelu.elsa.service.arkistointi.louhi

import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.common.SftpException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.integration.file.remote.session.Session
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class TampereLouhiServiceTest {

    private val sessionFactory = mock<SessionFactory<SftpClient.DirEntry>>()
    private val session = mock<Session<SftpClient.DirEntry>>()
    private val service = TampereLouhiService(sessionFactory)
    private var temporaryFile: Path? = null

    @AfterEach
    fun tearDown() {
        service.close()
        temporaryFile?.let(Files::deleteIfExists)
    }

    @ParameterizedTest
    @CsvSource(
        "false, ELSA",
        "true, YEK"
    )
    fun `laheta uploads through in-progress path and moves file to correct destination`(
        yek: Boolean,
        destinationFolder: String
    ) {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(arrayOf(mock()))
        var uploadedBytes = byteArrayOf()
        doAnswer { invocation ->
            uploadedBytes = invocation.getArgument<InputStream>(0).readBytes()
            Unit
        }.whenever(session).write(any(), any())
        val zipFile = createTemporaryZip("louhi-package")

        service.laheta(zipFile.toString(), yek)

        verify(session).write(any(), org.mockito.kotlin.eq("InProgress/${zipFile.fileName}"))
        verify(session).rename(
            "InProgress/${zipFile.fileName}",
            "Finished/$destinationFolder/${zipFile.fileName}"
        )
        assertThat(uploadedBytes).isEqualTo("louhi-package".toByteArray())
        assertThat(zipFile).doesNotExist()
        verify(session).close()
    }

    @Test
    fun `laheta fails when in-progress directory is unavailable and deletes temporary file`() {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(emptyArray())
        val zipFile = createTemporaryZip("unavailable-package")

        assertThatThrownBy {
            service.laheta(zipFile.toString(), false)
        }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasMessageContaining("Arkistointipalvelun hakemisto ei ole käytettävissä")

        verify(session, never()).write(any(), any())
        verify(session, never()).rename(any(), any())
        assertThat(zipFile).doesNotExist()
        verify(session).close()
    }

    @Test
    fun `laheta propagates upload failure without moving file and deletes temporary file`() {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(arrayOf(mock()))
        whenever(session.write(any(), any())).thenThrow(IllegalStateException("SFTP write failed"))
        val zipFile = createTemporaryZip("failed-package")

        assertThatThrownBy {
            service.laheta(zipFile.toString(), false)
        }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.publicationMayHaveStarted).isFalse()
                assertThat(it.cause).isNull()
                assertThat(it.message).doesNotContain("SFTP write failed")
            }

        verify(session, never()).rename(any(), any())
        assertThat(zipFile).doesNotExist()
        verify(session).close()
    }

    @Test
    fun `laheta fails before opening session when archive file does not exist`() {
        val missingFile = Files.createTempFile("missing-louhi-", ".zip")
        Files.delete(missingFile)

        assertThatThrownBy {
            service.laheta(missingFile.toString(), false)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Arkistointitiedostoa ei löydy")

        verify(sessionFactory, never()).session
    }

    @Test
    fun `session close failure does not replace confirmed transfer success`() {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(arrayOf(mock()))
        whenever(session.close()).thenThrow(IllegalStateException("private cleanup failure"))
        val file = createTemporaryZip("package")

        service.laheta(file.toString(), false)

        verify(session).rename(any(), any())
        assertThat(file).doesNotExist()
    }

    @Test
    fun `session close failure preserves uncertain publication result`() {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(arrayOf(mock()))
        whenever(session.rename(any(), any())).thenThrow(IllegalStateException("private transfer failure"))
        whenever(session.close()).thenThrow(IllegalStateException("private cleanup failure"))
        val file = createTemporaryZip("package")

        assertThatThrownBy { service.laheta(file.toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.publicationMayHaveStarted).isTrue()
                assertThat(it.cause).isNull()
                assertThat(it.message).doesNotContain("private")
            }
        assertThat(file).doesNotExist()
    }

    @ParameterizedTest
    @CsvSource(
        "WRITE,3,ACCESS_DENIED", "RENAME,3,ACCESS_DENIED", "RENAME,12,ACCESS_DENIED",
        "RENAME,2,PATH_NOT_FOUND", "RENAME,10,PATH_NOT_FOUND", "RENAME,11,TARGET_EXISTS",
        "RENAME,8,UNSUPPORTED", "RENAME,20,INVALID_FILENAME",
        "WRITE,14,STORAGE_UNAVAILABLE", "RENAME,14,STORAGE_UNAVAILABLE", "RENAME,15,STORAGE_UNAVAILABLE",
        "RENAME,17,REMOTE_BUSY", "RENAME,25,REMOTE_BUSY", "RENAME,26,REMOTE_BUSY",
        "WRITE,4,TRANSFER_FAILED", "RENAME,4,DELIVERY_UNCERTAIN", "RENAME,5,DELIVERY_UNCERTAIN",
        "RENAME,6,DELIVERY_UNCERTAIN", "RENAME,7,DELIVERY_UNCERTAIN", "RENAME,999,DELIVERY_UNCERTAIN"
    )
    fun `server responses are classified by status and operation without retaining private details`(
        stage: LouhiTransferStage, status: Int, expected: LouhiTransferFailure
    ) {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(arrayOf(mock()))
        val rejection = SftpException(status, "private filename person credentials")
        if (stage == LouhiTransferStage.WRITE) {
            whenever(session.write(any(), any())).thenThrow(rejection)
        } else {
            whenever(session.rename(any(), any())).thenThrow(rejection)
        }
        val file = createTemporaryZip("package")

        assertThatThrownBy { service.laheta(file.toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.failure).isEqualTo(expected)
                assertThat(it.stage).isEqualTo(stage)
                assertThat(it.sftpStatus).isEqualTo(status)
                assertThat(it.publicationMayHaveStarted).isEqualTo(expected == LouhiTransferFailure.DELIVERY_UNCERTAIN)
                assertThat(it.cause).isNull()
                assertThat(it.message).doesNotContain("private", "filename", "person", "credentials")
            }
        if (stage == LouhiTransferStage.WRITE) verify(session, never()).rename(any(), any())
        verify(session).close()
        assertThat(file).doesNotExist()
    }

    @Test
    fun `connection failure is safe and removes the local package`() {
        whenever(sessionFactory.session).thenThrow(IllegalStateException("private connection details"))
        val file = createTemporaryZip("package")

        assertThatThrownBy { service.laheta(file.toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.publicationMayHaveStarted).isFalse()
                assertThat(it.cause).isNull()
                assertThat(it.message).doesNotContain("private")
            }
        assertThat(file).doesNotExist()
    }

    @Test
    fun `shutdown during session acquisition closes the session without sending`() {
        whenever(sessionFactory.session).thenReturn(session)
        val file = createTemporaryZip("package")
        service.close()

        assertThatThrownBy { service.laheta(file.toString(), false, LouhiExecutionPolicy.ASYNC) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.publicationMayHaveStarted).isFalse()
            }
        verify(session).close()
        verify(session, never()).write(any(), any())
        assertThat(file).doesNotExist()
    }

    private fun createTemporaryZip(content: String): Path {
        return Files.createTempFile("louhi-test-", ".zip").also {
            temporaryFile = it
            Files.writeString(it, content)
        }
    }
}
