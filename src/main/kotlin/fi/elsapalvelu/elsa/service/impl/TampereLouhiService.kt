package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import org.apache.sshd.client.SshClient
import org.apache.sshd.common.keyprovider.FileKeyPairProvider
import org.apache.sshd.common.signature.BuiltinSignatures
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import kotlin.io.path.Path

@Service
class TampereLouhiService(
    resourceLoader: ResourceLoader,
    applicationProperties: ApplicationProperties
) {
    private val log = LoggerFactory.getLogger(TampereLouhiService::class.java)
    private val sessionFactory: DefaultSftpSessionFactory
    private val inProgress = "InProgress"
    private val finished = "Finished"

    init {
        val arkistointiProperties = applicationProperties.getArkistointi().getTre()
        val client = SshClient.setUpDefaultClient()
        client.signatureFactories = listOf(
            BuiltinSignatures.ed25519,
            BuiltinSignatures.ed25519_cert,
            BuiltinSignatures.sk_ssh_ed25519)
        val keys = FileKeyPairProvider(Path(resourceLoader.getResource(arkistointiProperties.privateKeyLocation!!).file.path))
        client.addPublicKeyIdentity(keys.loadKeys(null).iterator().next())

        sessionFactory = DefaultSftpSessionFactory(client, false)
        sessionFactory.setHost(arkistointiProperties.host)
        sessionFactory.setPort(arkistointiProperties.port?.toInt() ?: 22)
        sessionFactory.setUser(arkistointiProperties.user)
    }

    fun laheta(filePath: String) {
        try {
            sessionFactory.session.write(FileInputStream(filePath), inProgress)
            sessionFactory.session.rename("$inProgress/$filePath", "$finished/$filePath")
        } catch (e: Exception) {
            log.error("Virhe Tampereen arkistoinnissa tiedostolle $filePath", e)
        }
        val file = File(filePath)
        file.delete()
    }
}
