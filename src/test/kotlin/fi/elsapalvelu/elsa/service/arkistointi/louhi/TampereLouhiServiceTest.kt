package fi.elsapalvelu.elsa.service.arkistointi.louhi

import org.apache.sshd.sftp.client.SftpClient
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
    private var temporaryFile: Path? = null

    @AfterEach
    fun tearDown() {
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

        TampereLouhiService(sessionFactory).laheta(zipFile.toString(), yek)

        verify(session).write(any(), org.mockito.kotlin.eq("InProgress/${zipFile.fileName}"))
        verify(session).rename(
            "InProgress/${zipFile.fileName}",
            "Finished/$destinationFolder/${zipFile.fileName}"
        )
        assertThat(uploadedBytes).isEqualTo("louhi-package".toByteArray())
        assertThat(zipFile).doesNotExist()
    }

    @Test
    fun `laheta fails when in-progress directory is unavailable and deletes temporary file`() {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(emptyArray())
        val zipFile = createTemporaryZip("unavailable-package")

        assertThatThrownBy {
            TampereLouhiService(sessionFactory).laheta(zipFile.toString(), false)
        }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasMessageContaining("Arkistointipalvelun hakemisto ei ole käytettävissä")

        verify(session, never()).write(any(), any())
        verify(session, never()).rename(any(), any())
        assertThat(zipFile).doesNotExist()
    }

    @Test
    fun `laheta propagates upload failure without moving file and deletes temporary file`() {
        whenever(sessionFactory.session).thenReturn(session)
        whenever(session.list("InProgress")).thenReturn(arrayOf(mock()))
        whenever(session.write(any(), any())).thenThrow(IllegalStateException("SFTP write failed"))
        val zipFile = createTemporaryZip("failed-package")

        assertThatThrownBy {
            TampereLouhiService(sessionFactory).laheta(zipFile.toString(), false)
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("SFTP write failed")

        verify(session, never()).rename(any(), any())
        assertThat(zipFile).doesNotExist()
    }

    @Test
    fun `laheta fails before opening session when archive file does not exist`() {
        val missingFile = Files.createTempFile("missing-louhi-", ".zip")
        Files.delete(missingFile)

        assertThatThrownBy {
            TampereLouhiService(sessionFactory).laheta(missingFile.toString(), false)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Arkistointitiedostoa ei löydy")

        verify(sessionFactory, never()).session
    }

    private fun createTemporaryZip(content: String): Path {
        return Files.createTempFile("louhi-test-", ".zip").also {
            temporaryFile = it
            Files.writeString(it, content)
        }
    }
}
