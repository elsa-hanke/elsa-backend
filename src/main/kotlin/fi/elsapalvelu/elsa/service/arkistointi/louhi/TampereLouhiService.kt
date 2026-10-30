package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.LouhiTransferProperties
import jakarta.annotation.PreDestroy
import org.apache.sshd.client.SshClient
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.signature.BuiltinSignatures
import org.apache.sshd.common.util.security.SecurityUtils
import org.apache.sshd.sftp.client.SftpClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.file.remote.session.Session
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

enum class LouhiExecutionPolicy { SYNC, ASYNC }

@Service
class TampereLouhiService internal constructor(
    private val sessionFactory: SessionFactory<SftpClient.DirEntry>,
    private val transferProperties: LouhiTransferProperties = LouhiTransferProperties()
) {
    private val log = LoggerFactory.getLogger(TampereLouhiService::class.java)
    private val inProgress = "InProgress"
    private val finished = "Finished"
    private val yekFolder = "YEK"
    private val elsaFolder = "ELSA"
    private val deadlines = ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, "louhi-transfer-deadline").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    @Autowired
    constructor(
        resourceLoader: ResourceLoader,
        applicationProperties: ApplicationProperties
    ) : this(createSessionFactory(resourceLoader, applicationProperties), applicationProperties.getArkistointi().getTre().transfer)

    companion object {
        private fun createSessionFactory(
            resourceLoader: ResourceLoader,
            applicationProperties: ApplicationProperties
        ): DefaultSftpSessionFactory {
            val arkistointiProperties = applicationProperties.getArkistointi().getTre()
            arkistointiProperties.transfer.validate()
            val client = SshClient.setUpDefaultClient()
            client.signatureFactories = listOf(
                BuiltinSignatures.ed25519,
                BuiltinSignatures.ed25519_cert,
                BuiltinSignatures.sk_ssh_ed25519
            )

            arkistointiProperties.privateKeyLocation?.takeIf { it.isNotBlank() }?.let {
                val resource = resourceLoader.getResource(it)

                resource.inputStream.use { input ->
                    val keyPairs = SecurityUtils.loadKeyPairIdentities(
                        null,
                        NamedResource { it },
                        input,
                        FilePasswordProvider.EMPTY
                    )
                    client.addPublicKeyIdentity(keyPairs.iterator().next())
                }
            }

            return object : DefaultSftpSessionFactory(client, false) {
                override fun destroy() {
                    try {
                        super.destroy()
                    } finally {
                        client.stop()
                    }
                }
            }.apply {
                setHost(requireNotNull(arkistointiProperties.host))
                setPort(arkistointiProperties.port?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 22)
                setUser(requireNotNull(arkistointiProperties.user))
                setTimeout(arkistointiProperties.transfer.operationTimeout.toMillis().toInt())
            }
        }
    }

    fun laheta(filePath: String, yek: Boolean) = laheta(filePath, yek, LouhiExecutionPolicy.SYNC)

    fun laheta(filePath: String, yek: Boolean, policy: LouhiExecutionPolicy) {
        val file = File(filePath)
        require(file.exists()) {
            "Arkistointitiedostoa ei löydy"
        }
        try {
            send(file, yek, policy)
        } finally {
            val deleted = file.delete()
            if (!deleted) {
                log.warn("Louhi temporary file cleanup failed")
            }
        }
    }

    private fun send(file: File, yek: Boolean, policy: LouhiExecutionPolicy) {
        transferProperties.validate()
        val session = try {
            sessionFactory.session
        } catch (exception: Exception) {
            throw LouhiTransferException.from(exception, LouhiTransferStage.CONNECT)
        }
        val deadline = try {
            if (policy == LouhiExecutionPolicy.ASYNC) deadlines.schedule(
                { forceClose(session) }, transferProperties.transferTimeout.toMillis(), TimeUnit.MILLISECONDS
            ) else null
        } catch (exception: Exception) {
            closeSession(session)
            throw LouhiTransferException.from(exception, LouhiTransferStage.CONNECT)
        }
        var stage = LouhiTransferStage.LIST
        try {
            if (session.list(inProgress).isEmpty()) {
                throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Arkistointipalvelun hakemisto ei ole käytettävissä")
            }
            val targetPath = "$inProgress/${file.name}"
            val subFolder = if (yek) yekFolder else elsaFolder
            stage = LouhiTransferStage.WRITE
            FileInputStream(file).use { session.write(it, targetPath) }
            check(!Thread.currentThread().isInterrupted)
            // Includes any destination removal performed internally by Session.rename, not only the final move.
            stage = LouhiTransferStage.RENAME
            session.rename(targetPath, "$finished/$subFolder/${file.name}")
            log.info("Louhi package transfer completed")
        } catch (exception: ResponseStatusException) {
            throw exception
        } catch (exception: Exception) {
            throw LouhiTransferException.from(exception, stage)
        } finally {
            deadline?.cancel(false)
            closeSession(session)
        }
    }

    private fun closeSession(session: Session<SftpClient.DirEntry>) {
        try {
            session.close()
        } catch (_: Exception) {
            // Cleanup must not replace a known success or an uncertain transfer outcome.
            log.warn("Louhi session cleanup failed")
            forceClose(session)
        }
    }

    private fun forceClose(session: Session<SftpClient.DirEntry>) {
        try {
            (session.clientInstance as? SftpClient)?.session?.close(true)
        } catch (_: Exception) {
            log.warn("Louhi SSH connection cleanup failed")
        }
    }

    @PreDestroy
    fun close() {
        deadlines.shutdownNow()
        (sessionFactory as? DefaultSftpSessionFactory)?.destroy()
    }
}
