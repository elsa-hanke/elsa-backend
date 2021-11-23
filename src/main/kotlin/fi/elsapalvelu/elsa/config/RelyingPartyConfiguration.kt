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
import org.springframework.security.saml2.provider.service.registration.*
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
        val registrations = mutableListOf<RelyingPartyRegistration>()

        // Suomi.fi
        if (applicationProperties.getSecurity().getSuomifi().enabled == true) {
            val publicKeyResource: Resource =
                resourceLoader.getResource(
                    applicationProperties.getSecurity().getSuomifi().samlCertificateLocation!!
                )
            val certificate: X509Certificate =
                X509Support.decodeCertificate(publicKeyResource.inputStream.readBytes())!!

            val privateKeyResource: Resource =
                resourceLoader.getResource(
                    applicationProperties.getSecurity().getSuomifi().samlPrivateKeyLocation!!
                )
            val rsa: RSAPrivateKey =
                RsaKeyConverters.pkcs8().convert(privateKeyResource.inputStream)!!

            val signingCredential: Saml2X509Credential =
                Saml2X509Credential.signing(rsa, certificate)
            val decryptionCredential: Saml2X509Credential =
                Saml2X509Credential.decryption(rsa, certificate)

            registrations.add(
                createRegistration(
                    applicationProperties.getSecurity().getSuomifi().samlSuomifiMetadataLocation!!,
                    signingCredential,
                    decryptionCredential,
                    "suomifi",
                    applicationProperties.getSecurity().getSuomifi().samlSuomifiEntityId!!,
                    applicationProperties.getSecurity().getSuomifi().samlSuomifiSloUrl!!
                )
            )
        }

        // Haka
        if (applicationProperties.getSecurity().getHaka().enabled == true) {
            val publicKeyResource: Resource =
                resourceLoader.getResource(
                    applicationProperties.getSecurity().getHaka().samlCertificateLocation!!
                )
            val certificate: X509Certificate =
                X509Support.decodeCertificate(publicKeyResource.inputStream.readBytes())!!

            val privateKeyResource: Resource =
                resourceLoader.getResource(
                    applicationProperties.getSecurity().getHaka().samlPrivateKeyLocation!!
                )
            val rsa: RSAPrivateKey =
                RsaKeyConverters.pkcs8().convert(privateKeyResource.inputStream)!!

            val signingCredential: Saml2X509Credential =
                Saml2X509Credential.signing(rsa, certificate)
            val decryptionCredential: Saml2X509Credential =
                Saml2X509Credential.decryption(rsa, certificate)

            yliopistoRepository.findAllHaka().forEach {
                registrations.add(
                    createRegistration(
                        applicationProperties.getSecurity().getHaka().samlHakaMetadataLocation!!,
                        signingCredential,
                        decryptionCredential,
                        it.hakaId!!,
                        it.hakaEntityId!!,
                        it.hakaSLOLocation!!
                    )
                )
            }
        }

        return if (registrations.size > 0) InMemoryRelyingPartyRegistrationRepository(registrations) else null
    }

    private fun createRegistration(
        metadataLocation: String,
        signingCredential: Saml2X509Credential,
        decryptionCredential: Saml2X509Credential,
        registrationId: String,
        entityId: String,
        sloUrl: String
    ): RelyingPartyRegistration {
        return RelyingPartyRegistrations
            .fromMetadataLocation(metadataLocation)
            .registrationId(registrationId)
            .assertingPartyDetails { party: RelyingPartyRegistration.AssertingPartyDetails.Builder ->
                party.entityId(entityId)
                party.singleLogoutServiceLocation(sloUrl)
                party.singleLogoutServiceBinding(Saml2MessageBinding.REDIRECT)
            }
            .signingX509Credentials { signing -> signing.add(signingCredential) }
            .decryptionX509Credentials { decryption -> decryption.add(decryptionCredential) }
            .singleLogoutServiceBinding(Saml2MessageBinding.REDIRECT)
            .build()
    }
}
