package fi.elsapalvelu.elsa.config

import org.opensaml.security.x509.X509Support
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey


@Configuration
@Profile("dev", "prod")
class RelyingPartyConfiguration(private val applicationProperties: ApplicationProperties) {

    @Autowired
    private val resourceLoader: ResourceLoader? = null

    @Bean
    @Throws(Exception::class)
    fun relyingPartyRegistrations(): RelyingPartyRegistrationRepository? {
        val publicKeyResource: Resource =
            resourceLoader!!.getResource(applicationProperties.getSecurity().samlCertificateLocation!!)
        val publicKeyFile = File(publicKeyResource.filename!!)

        publicKeyResource.inputStream.use { inputStream ->
            Files.copy(
                inputStream, publicKeyFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        val certificate: X509Certificate = X509Support.decodeCertificate(publicKeyFile)!!

        val privateKeyResource: Resource =
            resourceLoader.getResource(applicationProperties.getSecurity().samlPrivateKeyLocation!!)
        val rsa: RSAPrivateKey = RsaKeyConverters.pkcs8().convert(privateKeyResource.inputStream)!!

        val signingCredential: Saml2X509Credential = Saml2X509Credential.signing(rsa, certificate)
        val decryptionCredential: Saml2X509Credential =
            Saml2X509Credential.decryption(rsa, certificate)

        val resource: Resource = ClassPathResource("suomifi.crt")
        val verificationCertificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(resource.inputStream) as X509Certificate
        val verificationCredential = Saml2X509Credential.verification(verificationCertificate)

        val registration = RelyingPartyRegistration
            .withRegistrationId("suomifi")
            .assertingPartyDetails { party: AssertingPartyDetails.Builder ->
                party
                    .entityId("https://testi.apro.tunnistus.fi/idp1")
                    .singleSignOnServiceLocation("https://testi.apro.tunnistus.fi/idp/profile/SAML2/Redirect/SSO")
                    .wantAuthnRequestsSigned(true)
                    .verificationX509Credentials { c: MutableCollection<Saml2X509Credential?> ->
                        c.add(
                            verificationCredential
                        )
                    }
            }
            .signingX509Credentials { c: MutableCollection<Saml2X509Credential?> ->
                c.add(
                    signingCredential
                )
            }
            .decryptionX509Credentials { c: MutableCollection<Saml2X509Credential?> ->
                c.add(
                    decryptionCredential
                )
            }
            .build()
        return InMemoryRelyingPartyRegistrationRepository(registration)
    }
}
