package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.repository.YliopistoRepository
import org.opensaml.security.x509.X509Support
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey


@Configuration
@Profile("dev", "prod")
class RelyingPartyConfiguration(
    private val resourceLoader: ResourceLoader,
    private val applicationProperties: ApplicationProperties,
    private val yliopistoRepository: YliopistoRepository
) {

    @Bean
    @Throws(Exception::class)
    fun relyingPartyRegistrations(): RelyingPartyRegistrationRepository? {
        val publicKeyResource: Resource =
            resourceLoader.getResource(applicationProperties.getSecurity().samlCertificateLocation!!)
        val certificate: X509Certificate =
            X509Support.decodeCertificate(publicKeyResource.inputStream.readBytes())!!

        val privateKeyResource: Resource =
            resourceLoader.getResource(applicationProperties.getSecurity().samlPrivateKeyLocation!!)
        val rsa: RSAPrivateKey = RsaKeyConverters.pkcs8().convert(privateKeyResource.inputStream)!!

        val signingCredential: Saml2X509Credential = Saml2X509Credential.signing(rsa, certificate)
        val decryptionCredential: Saml2X509Credential =
            Saml2X509Credential.decryption(rsa, certificate)

        val registrations = mutableListOf<RelyingPartyRegistration>()

        // Suomi.fi
        registrations.add(
            createRegistration(
                applicationProperties.getSecurity().samlSuomifiCertificateLocation!!,
                signingCredential,
                decryptionCredential,
                "suomifi",
                "https://testi.apro.tunnistus.fi/idp1",
                "https://testi.apro.tunnistus.fi/idp/profile/SAML2/Redirect/SSO"
            )
        )

        // Haka
        yliopistoRepository.findAllHaka().forEach {
            registrations.add(
                createRegistration(
                    applicationProperties.getSecurity().samlHakaCertificateLocation!! + it.hakaId + ".crt",
                    signingCredential,
                    decryptionCredential,
                    it.hakaId!!,
                    it.hakaEntityId!!,
                    it.hakaSSOLocation!!
                )
            )
        }

        return InMemoryRelyingPartyRegistrationRepository(registrations)
    }

    private fun createRegistration(
        verificationCredentialLocation: String,
        signingCredential: Saml2X509Credential,
        decryptionCredential: Saml2X509Credential,
        registrationId: String,
        entityId: String,
        ssoLocation: String
    ): RelyingPartyRegistration {
        val resource: Resource = resourceLoader.getResource(verificationCredentialLocation)
        val verification = CertificateFactory.getInstance("X.509")
            .generateCertificate(resource.inputStream) as X509Certificate
        val verificationCredential = Saml2X509Credential.verification(verification)

        return RelyingPartyRegistration
            .withRegistrationId(registrationId)
            .assertingPartyDetails { party: AssertingPartyDetails.Builder ->
                party
                    .entityId(entityId)
                    .singleSignOnServiceLocation(ssoLocation)
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
    }
}
