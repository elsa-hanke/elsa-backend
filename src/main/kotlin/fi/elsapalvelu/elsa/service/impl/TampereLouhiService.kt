package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import org.apache.sshd.client.SshClient
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.signature.BuiltinSignatures
import org.apache.sshd.common.util.security.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream

@Service
class TampereLouhiService(
    resourceLoader: ResourceLoader,
    applicationProperties: ApplicationProperties
) {
    private val log = LoggerFactory.getLogger(TampereLouhiService::class.java)
    private val sessionFactory: DefaultSftpSessionFactory
    private val inProgress = "InProgress"
    private val finished = "Finished"
    private val yekFolder = "YEK"
    private val elsaFolder = "ELSA"

    init {
        val arkistointiProperties = applicationProperties.getArkistointi().getTre()
        val client = SshClient.setUpDefaultClient()
        client.signatureFactories = listOf(
            BuiltinSignatures.ed25519,
            BuiltinSignatures.ed25519_cert,
            BuiltinSignatures.sk_ssh_ed25519)

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

        sessionFactory = DefaultSftpSessionFactory(client, false)
        sessionFactory.setHost(arkistointiProperties.host)
        sessionFactory.setPort(arkistointiProperties.port?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 22)
        sessionFactory.setUser(arkistointiProperties.user)
    }

    fun laheta(filePath: String, yek: Boolean) {
        try {
            val session = sessionFactory.session
            val file = File(filePath)
            val targetPath = "$inProgress/${file.name}"

            if (session.list(inProgress).isEmpty()) {
                log.error("Hakemistoa ei löydy: $inProgress")
                return
            }

            val subFolder = if (yek) yekFolder else elsaFolder

            session.write(FileInputStream(filePath), targetPath)
            session.rename(targetPath, "$finished/${subFolder}/${File(filePath).name}")
        } catch (e: Exception) {
            log.error("Virhe Tampereen arkistoinnissa tiedostolle $filePath", e)
        }
        val file = File(filePath)
        val deleted = file.delete()
        if (!deleted) {
            log.error("Virhe tiedoston ${file.name} poistamisessa")
        }
    }
}
