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
import java.io.*
import java.net.URL
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamConstants.END_ELEMENT
import javax.xml.stream.XMLStreamConstants.START_ELEMENT
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement


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
                RelyingPartyRegistrations
                    .fromMetadataLocation(
                        applicationProperties.getSecurity()
                            .getSuomifi().samlSuomifiMetadataLocation!!
                    )
                    .registrationId("suomifi")
                    .assertingPartyDetails { party: RelyingPartyRegistration.AssertingPartyDetails.Builder ->
                        party.entityId(
                            applicationProperties.getSecurity().getSuomifi().samlSuomifiEntityId!!
                        )
                    }
                    .signingX509Credentials { signing -> signing.add(signingCredential) }
                    .decryptionX509Credentials { decryption -> decryption.add(decryptionCredential) }
                    .singleLogoutServiceBinding(Saml2MessageBinding.REDIRECT)
                    .build()
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
                    RelyingPartyRegistrations
                        .fromMetadata(ByteArrayInputStream(parseHakaFile(it.hakaEntityId!!).toByteArray()))
                        .registrationId(it.hakaId!!)
                        .assertingPartyDetails { party: RelyingPartyRegistration.AssertingPartyDetails.Builder ->
                            party.entityId(it.hakaEntityId!!)
                        }
                        .signingX509Credentials { signing -> signing.add(signingCredential) }
                        .decryptionX509Credentials { decryption ->
                            decryption.add(
                                decryptionCredential
                            )
                        }
                        .singleLogoutServiceBinding(Saml2MessageBinding.REDIRECT)
                        .build()
                )
            }
        }

        return if (registrations.size > 0) InMemoryRelyingPartyRegistrationRepository(registrations) else null
    }

    private fun parseHakaFile(entityId: String): String {
        val url = URL(applicationProperties.getSecurity().getHaka().samlHakaMetadataLocation!!)
        val stream: InputStream = url.openStream()

        val inputFactory = XMLInputFactory.newInstance()
        val outputFactory = XMLOutputFactory.newInstance()
        val reader = inputFactory.createXMLEventReader(stream)
        var writer: XMLEventWriter? = null
        var writeElement = false
        val outputStream = ByteArrayOutputStream()

        while (reader.hasNext()) {
            val event = reader.nextEvent()

            when (event.eventType) {
                START_ELEMENT -> {
                    val startEvent = event as StartElement
                    if (startEvent.name.localPart.equals("EntitiesDescriptor")) {
                        writer = outputFactory.createXMLEventWriter(outputStream)
                        writer?.add(event)
                    } else if (startEvent.name.localPart.equals("EntityDescriptor")) {
                        startEvent.attributes.forEach {
                            if (it.name.localPart == "entityID" && it.value == entityId) {
                                writeElement = true
                                writer?.add(event)
                            }
                        }
                    } else if (writeElement) writer?.add(event)
                }
                END_ELEMENT -> {
                    val endEvent = event as EndElement
                    if (endEvent.name.localPart.equals("EntityDescriptor") && writeElement) {
                        writeElement = false
                        writer?.add(event)
                    } else if (endEvent.name.localPart.equals("EntitiesDescriptor")) {
                        writer?.add(event)
                        writer?.close()
                        writer = null
                    } else if (writeElement) writer?.add(event)
                }
                else -> if (writeElement) writer?.add(event)
            }
        }
        reader.close()
        writer?.close()
        stream.close()
        return outputStream.toString()
    }
}
