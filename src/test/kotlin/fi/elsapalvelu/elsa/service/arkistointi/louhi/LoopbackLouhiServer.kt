package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.config.arkistointi.LouhiTransferProperties
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import java.nio.file.Files
import java.nio.file.Path

/** Owned loopback server: no university endpoint, production credentials or production data. */
class LoopbackLouhiServer : AutoCloseable {
    val directory: Path = Files.createTempDirectory("elsa-louhi-it-")
    val remote: Path = Files.createDirectory(directory.resolve("remote"))
    val subsystem: SftpSubsystemFactory = SftpSubsystemFactory.Builder().build()
    private val server: SshServer

    init {
        listOf("InProgress", "Finished/ELSA", "Finished/YEK").forEach { Files.createDirectories(remote.resolve(it)) }
        Files.writeString(remote.resolve("InProgress/.keep"), "")
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

    fun client(onClose: () -> Unit = {}): TampereLouhiService {
        val factory = object : DefaultSftpSessionFactory() {
            override fun destroy() {
                try {
                    super.destroy()
                } finally {
                    onClose()
                }
            }
        }.apply {
            setHost("127.0.0.1")
            setPort(server.port)
            setUser("test")
            setPassword("test")
            setAllowUnknownKeys(true)
            setTimeout(10000)
        }
        return TampereLouhiService(factory, LouhiTransferProperties())
    }

    override fun close() {
        server.stop(true)
        Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }
}
