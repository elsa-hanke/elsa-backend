package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.config.arkistointi.LouhiTransferProperties
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.sftp.server.FileHandle
import org.apache.sshd.sftp.server.SftpEventListener
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import org.apache.sshd.sftp.common.SftpConstants
import org.apache.sshd.sftp.common.SftpException
import org.apache.sshd.sftp.SftpModuleProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Real production sender against loopback SSH/SFTP only. No university systems are contacted. */
class TampereLouhiTransferIT {
    @TempDir lateinit var directory: Path
    private lateinit var server: SshServer
    private lateinit var subsystem: SftpSubsystemFactory
    private lateinit var remote: Path
    private val clients = mutableListOf<TampereLouhiService>()
    private val legacyFilename = "valmistuminen_matti_meikäläinen.zip"

    @BeforeEach
    fun startServer() {
        remote = Files.createDirectory(directory.resolve("remote"))
        listOf("InProgress", "Finished/ELSA", "Finished/YEK").forEach { Files.createDirectories(remote.resolve(it)) }
        Files.writeString(remote.resolve("InProgress/.keep"), "")
        subsystem = SftpSubsystemFactory.Builder().build()
        server = SshServer.setUpDefaultServer().apply {
            host = "127.0.0.1"
            port = 0
            keyPairProvider = SimpleGeneratorHostKeyProvider(directory.resolve("hostkey"))
            passwordAuthenticator = org.apache.sshd.server.auth.password.PasswordAuthenticator { user, password, _ ->
                user == "test" && password == "test"
            }
            subsystemFactories = listOf(subsystem)
            fileSystemFactory = VirtualFileSystemFactory(remote)
            start()
        }
    }

    @AfterEach
    fun stopServer() {
        clients.forEach { it.close() }
        server.stop(true)
    }

    @ParameterizedTest
    @CsvSource("false,ELSA", "true,YEK")
    fun `production sender preserves legacy filename and directories`(yek: Boolean, folder: String) {
        val file = source()
        val bytes = Files.readAllBytes(file)

        client().laheta(file.toString(), yek)

        assertThat(Files.readAllBytes(remote.resolve("Finished/$folder/$legacyFilename"))).isEqualTo(bytes)
        assertThat(remote.resolve("InProgress/$legacyFilename")).doesNotExist()
        assertThat(file).doesNotExist()
        Files.list(remote.resolve("InProgress")).use {
            assertThat(it.map { entry -> entry.fileName.toString() }.toList()).containsExactly(".keep")
        }
    }

    @Test
    fun `failed write is classified before publication and local file is removed`() {
        val firstWrite = AtomicBoolean(true)
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun writing(session: ServerSession, handle: String, file: FileHandle,
                offset: Long, data: ByteArray, dataOffset: Int, dataLen: Int) {
                if (firstWrite.getAndSet(false)) throw IOException("simulated storage failure")
            }
        })
        val file = source()

        assertThatThrownBy { client().laheta(file.toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.publicationMayHaveStarted).isFalse()
            }
        assertThat(remote.resolve("Finished/ELSA/$legacyFilename")).doesNotExist()
        assertThat(file).doesNotExist()
        client().laheta(source().toString(), false)
        assertThat(remote.resolve("Finished/ELSA/$legacyFilename")).exists()
    }

    @Test
    fun `lost rename acknowledgement is uncertain even when receiver has the ZIP`() {
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun moved(session: ServerSession, source: Path, target: Path,
                options: Collection<CopyOption>, thrown: Throwable?) {
                if (thrown == null) session.close(true)
            }
        })
        val file = source()

        assertThatThrownBy { client().laheta(file.toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.publicationMayHaveStarted).isTrue()
                assertThat(it.cause).isNull()
            }
        assertThat(remote.resolve("Finished/ELSA/$legacyFilename")).exists()
        assertThat(file).doesNotExist()
    }

    @ParameterizedTest
    @ValueSource(strings = ["permission", "missing-path"])
    fun `server rejects this rename invocation with a classified permanent failure`(reason: String) {
        if (reason == "permission") {
            subsystem.addSftpEventListener(object : SftpEventListener {
                override fun moving(session: ServerSession, source: Path, target: Path, options: Collection<CopyOption>) {
                    throw AccessDeniedException("private-person.zip")
                }
            })
        } else {
            Files.delete(remote.resolve("Finished/ELSA"))
        }
        val file = source()

        assertThatThrownBy { client().laheta(file.toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.stage).isEqualTo(LouhiTransferStage.RENAME)
                assertThat(it.failure).isEqualTo(if (reason == "permission") {
                    LouhiTransferFailure.ACCESS_DENIED
                } else {
                    LouhiTransferFailure.PATH_NOT_FOUND
                })
                assertThat(it.sftpStatus).isIn(if (reason == "permission") {
                    listOf(SftpConstants.SSH_FX_PERMISSION_DENIED)
                } else {
                    listOf(SftpConstants.SSH_FX_NO_SUCH_FILE, SftpConstants.SSH_FX_NO_SUCH_PATH)
                })
                assertThat(it.failure.retryable).isFalse()
                assertThat(it.publicationMayHaveStarted).isFalse()
                assertThat(it.cause).isNull()
                assertThat(it.message).doesNotContain("private", legacyFilename)
            }
        assertThat(remote.resolve("InProgress/$legacyFilename")).exists()
        assertThat(remote.resolve("Finished/ELSA/$legacyFilename")).doesNotExist()
        assertThat(file).doesNotExist()
    }

    @ParameterizedTest
    @ValueSource(ints = [3, 6])
    fun `legacy rename replaces an existing package without a target-exists refusal`(version: Int) {
        SftpModuleProperties.SFTP_VERSION.set(server, version)
        val destination = remote.resolve("Finished/ELSA/$legacyFilename")
        Files.writeString(destination, "previous unconsumed package")
        val removed = AtomicBoolean(false)
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun removed(session: ServerSession, path: Path, isDirectory: Boolean, thrown: Throwable?) {
                if (path.fileName.toString() == legacyFilename && thrown == null) removed.set(true)
            }
        })

        client().laheta(source().toString(), false)

        assertThat(Files.readString(destination)).isEqualTo("immutable ZIP bytes")
        assertThat(removed.get()).isEqualTo(version < 5)
    }

    @ParameterizedTest
    @ValueSource(strings = ["DELETE", "MOVE"])
    fun `generic failure in either internal rename step remains uncertain`(step: String) {
        SftpModuleProperties.SFTP_VERSION.set(server, 3)
        val destination = remote.resolve("Finished/ELSA/$legacyFilename")
        Files.writeString(destination, "previous unconsumed package")
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun removing(session: ServerSession, path: Path, isDirectory: Boolean) {
                if (step == "DELETE" && path.fileName.toString() == legacyFilename) {
                    throw SftpException(SftpConstants.SSH_FX_FAILURE, "private deletion details")
                }
            }
            override fun moving(session: ServerSession, source: Path, target: Path, options: Collection<CopyOption>) {
                if (step == "MOVE") throw SftpException(SftpConstants.SSH_FX_FAILURE, "private move details")
            }
        })

        assertThatThrownBy { client().laheta(source().toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.stage).isEqualTo(LouhiTransferStage.RENAME)
                assertThat(it.sftpStatus).isEqualTo(SftpConstants.SSH_FX_FAILURE)
                assertThat(it.failure).isEqualTo(LouhiTransferFailure.DELIVERY_UNCERTAIN)
                assertThat(it.failure.retryable).isFalse()
                assertThat(it.message).doesNotContain("private", legacyFilename)
            }
        if (step == "DELETE") {
            assertThat(Files.readString(destination)).isEqualTo("previous unconsumed package")
        } else {
            assertThat(destination).doesNotExist()
        }
        assertThat(remote.resolve("InProgress/$legacyFilename")).exists()
    }

    @Test
    fun `missing source refusal does not prove the common package was never published`() {
        SftpModuleProperties.SFTP_VERSION.set(server, 3)
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun moving(session: ServerSession, source: Path, target: Path, options: Collection<CopyOption>) {
                // Deterministically model another execution moving the shared source before this move runs.
                Files.move(source, target)
                throw NoSuchFileException("private source details")
            }
        })

        assertThatThrownBy { client().laheta(source().toString(), false) }
            .isInstanceOfSatisfying(LouhiTransferException::class.java) {
                assertThat(it.failure).isEqualTo(LouhiTransferFailure.PATH_NOT_FOUND)
                assertThat(it.failure.retryable).isFalse()
                // This flag describes our classification, not absence of publication by another execution.
                assertThat(it.publicationMayHaveStarted).isFalse()
                assertThat(it.message).doesNotContain("private", legacyFilename)
            }
        assertThat(Files.readString(remote.resolve("Finished/ELSA/$legacyFilename")))
            .isEqualTo("immutable ZIP bytes")
        assertThat(remote.resolve("InProgress/$legacyFilename")).doesNotExist()
    }

    @Test
    fun `legacy transport alone cannot deduplicate retry after receiver consumes ZIP`() {
        client().laheta(source().toString(), false)
        val destination = remote.resolve("Finished/ELSA/$legacyFilename")
        Files.delete(destination)

        // Models another adapter call after remote success followed by a failed local commit or process crash.
        client().laheta(source().toString(), false)

        assertThat(destination).exists()
        // Accepted legacy limitation, not evidence of receiver-side idempotency.
    }

    @ParameterizedTest
    @ValueSource(strings = ["WRITE", "RENAME"])
    fun `total deadline closes real SSH connection and preserves publication uncertainty`(stage: LouhiTransferStage) {
        val blocked = CountDownLatch(1)
        val release = CountDownLatch(1)
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun writing(session: ServerSession, handle: String, file: FileHandle,
                offset: Long, data: ByteArray, dataOffset: Int, dataLen: Int) {
                if (stage == LouhiTransferStage.WRITE) {
                    blocked.countDown()
                    check(release.await(15, TimeUnit.SECONDS))
                }
            }
            override fun moving(session: ServerSession, source: Path, target: Path, options: Collection<CopyOption>) {
                if (stage == LouhiTransferStage.RENAME) {
                    blocked.countDown()
                    check(release.await(15, TimeUnit.SECONDS))
                }
            }
        })
        val executor = Executors.newSingleThreadExecutor()
        val sender = client(Duration.ofSeconds(2))
        val file = source()
        try {
            val running = executor.submit { sender.laheta(file.toString(), false, LouhiExecutionPolicy.ASYNC) }
            assertThat(blocked.await(10, TimeUnit.SECONDS)).isTrue()
            val failure = assertThrows<ExecutionException> { running.get(7, TimeUnit.SECONDS) }
            assertThat(failure.cause)
                .isInstanceOfSatisfying(LouhiTransferException::class.java) { transfer ->
                    assertThat(transfer.stage).isEqualTo(stage)
                    assertThat(transfer.publicationMayHaveStarted).isEqualTo(stage == LouhiTransferStage.RENAME)
                }
            assertThat(remote.resolve("Finished/ELSA/$legacyFilename")).doesNotExist()
            assertThat(file).doesNotExist()
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `sync transfer can finish after the configured async deadline`() {
        val blocked = CountDownLatch(1)
        val release = CountDownLatch(1)
        subsystem.addSftpEventListener(object : SftpEventListener {
            override fun writing(session: ServerSession, handle: String, file: FileHandle,
                offset: Long, data: ByteArray, dataOffset: Int, dataLen: Int) {
                blocked.countDown()
                check(release.await(15, TimeUnit.SECONDS))
            }
        })
        val executor = Executors.newSingleThreadExecutor()
        val sender = client(Duration.ofMillis(200))
        val file = source()
        try {
            val running = executor.submit { sender.laheta(file.toString(), false) }
            assertThat(blocked.await(10, TimeUnit.SECONDS)).isTrue()
            assertThrows<java.util.concurrent.TimeoutException> { running.get(1, TimeUnit.SECONDS) }
            release.countDown()
            running.get(7, TimeUnit.SECONDS)
            assertThat(Files.readString(remote.resolve("Finished/ELSA/$legacyFilename")))
                .isEqualTo("immutable ZIP bytes")
            assertThat(file).doesNotExist()
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private fun source(): Path = Files.writeString(directory.resolve(legacyFilename), "immutable ZIP bytes")

    private fun client(timeout: Duration = Duration.ofSeconds(30)): TampereLouhiService {
        val factory = DefaultSftpSessionFactory().apply {
            setHost("127.0.0.1")
            setPort(server.port)
            setUser("test")
            setPassword("test")
            setAllowUnknownKeys(true) // Loopback test server only.
            setTimeout(10000)
        }
        return TampereLouhiService(factory, LouhiTransferProperties().apply {
            transferTimeout = timeout
        }).also(clients::add)
    }
}
