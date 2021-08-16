package fi.elsapalvelu.elsa.security.logout

import net.shibboleth.utilities.java.support.resolver.CriteriaSet
import net.shibboleth.utilities.java.support.xml.SerializeSupport
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.io.Marshaller
import org.opensaml.core.xml.io.MarshallingException
import org.opensaml.saml.security.impl.SAMLMetadataSignatureSigningParametersResolver
import org.opensaml.security.credential.BasicCredential
import org.opensaml.security.credential.Credential
import org.opensaml.security.credential.CredentialSupport
import org.opensaml.security.credential.UsageType
import org.opensaml.xmlsec.SignatureSigningParameters
import org.opensaml.xmlsec.SignatureSigningParametersResolver
import org.opensaml.xmlsec.criterion.SignatureSigningConfigurationCriterion
import org.opensaml.xmlsec.crypto.XMLSigningUtil
import org.opensaml.xmlsec.impl.BasicSignatureSigningConfiguration
import org.opensaml.xmlsec.signature.SignableXMLObject
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.SignatureSupport
import org.springframework.security.saml2.Saml2Exception
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.util.Assert
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import org.w3c.dom.Element
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


class OpenSamlSigningUtils {

    companion object {
        fun serialize(`object`: XMLObject?): String? {
            return try {
                val marshaller: Marshaller? =
                    XMLObjectProviderRegistrySupport.getMarshallerFactory()
                        .getMarshaller(`object`!!)
                val element: Element? = marshaller?.marshall(`object`)
                SerializeSupport.nodeToString(element!!)
            } catch (ex: MarshallingException) {
                throw Saml2Exception(ex)
            }
        }

        fun <O : SignableXMLObject?> sign(
            `object`: O,
            relyingPartyRegistration: RelyingPartyRegistration
        ): O {
            val parameters = resolveSigningParameters(relyingPartyRegistration)
            return try {
                SignatureSupport.signObject(`object`!!, parameters!!)
                `object`
            } catch (ex: Exception) {
                throw Saml2Exception(ex)
            }
        }

        fun sign(registration: RelyingPartyRegistration?): QueryParametersPartial {
            return QueryParametersPartial(registration)
        }

        private fun resolveSigningParameters(
            relyingPartyRegistration: RelyingPartyRegistration
        ): SignatureSigningParameters? {
            val credentials: List<Credential> = resolveSigningCredentials(relyingPartyRegistration)
            val algorithms = relyingPartyRegistration.assertingPartyDetails.signingAlgorithms
            val digests: List<String> =
                Collections.singletonList(SignatureConstants.ALGO_ID_DIGEST_SHA256)
            val canonicalization = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS
            val resolver: SignatureSigningParametersResolver =
                SAMLMetadataSignatureSigningParametersResolver()
            val criteria = CriteriaSet()
            val signingConfiguration = BasicSignatureSigningConfiguration()
            signingConfiguration.setSigningCredentials(credentials)
            signingConfiguration.setSignatureAlgorithms(algorithms)
            signingConfiguration.setSignatureReferenceDigestMethods(digests)
            signingConfiguration.signatureCanonicalizationAlgorithm = canonicalization
            criteria.add(SignatureSigningConfigurationCriterion(signingConfiguration))
            return try {
                val parameters = resolver.resolveSingle(criteria)
                Assert.notNull(parameters, "Failed to resolve any signing credential")
                parameters
            } catch (ex: Exception) {
                throw Saml2Exception(ex)
            }
        }

        private fun resolveSigningCredentials(relyingPartyRegistration: RelyingPartyRegistration): List<Credential> {
            val credentials: MutableList<Credential> = ArrayList()
            for (x509Credential in relyingPartyRegistration.signingX509Credentials) {
                val certificate: X509Certificate = x509Credential.certificate
                val privateKey = x509Credential.privateKey
                val credential: BasicCredential =
                    CredentialSupport.getSimpleCredential(certificate, privateKey)
                credential.entityId = relyingPartyRegistration.entityId
                credential.setUsageType(UsageType.SIGNING)
                credentials.add(credential)
            }
            return credentials
        }

        class QueryParametersPartial(private val registration: RelyingPartyRegistration?) {
            private val components: MutableMap<String, String?> = LinkedHashMap()
            fun param(key: String, value: String?): QueryParametersPartial {
                components[key] = value
                return this
            }

            fun parameters(): Map<String, String?> {
                val parameters: SignatureSigningParameters? =
                    resolveSigningParameters(registration!!)
                val credential: Credential? = parameters?.signingCredential
                val algorithmUri = parameters?.signatureAlgorithm
                components["SigAlg"] = algorithmUri
                val builder = UriComponentsBuilder.newInstance()
                for ((key, value) in components) {
                    builder.queryParam(
                        key,
                        UriUtils.encode(value!!, StandardCharsets.ISO_8859_1)
                    )
                }
                val queryString = builder.build(true).toString().substring(1)
                try {
                    val rawSignature = XMLSigningUtil.signWithURI(
                        credential!!,
                        algorithmUri!!,
                        queryString.toByteArray(StandardCharsets.UTF_8)
                    )
                    val b64Signature: String? = Saml2Utils.samlEncode(rawSignature)
                    components["Signature"] = b64Signature
                } catch (ex: SecurityException) {
                    throw Saml2Exception(ex)
                }
                return components
            }
        }
    }
}
