package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.interceptor.OkHttp3RequestInterceptor
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.opensaml.security.x509.X509Support
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.stereotype.Service
import java.security.KeyPair
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.util.concurrent.TimeUnit

@Qualifier("SisuHy")
@Service
class SisuHyClientBuilderImpl(
    applicationProperties: ApplicationProperties,
    resourceLoader: ResourceLoader
) : GraphQLClientBuilder {

    init {
        Companion.applicationProperties = applicationProperties
        Companion.resourceLoader = resourceLoader
    }

    companion object {

        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var resourceLoader: ResourceLoader

        val okHttpClient: OkHttpClient by lazy {
            val publicKeyResource: Resource =
                resourceLoader.getResource(
                    applicationProperties.getSecurity().getSisuHy().certificateLocation!!
                )
            val bytes = publicKeyResource.inputStream.use { it.readBytes() }
            val certificate: X509Certificate =
                X509Support.decodeCertificate(bytes)!!
            val privateKeyResource: Resource =
                resourceLoader.getResource(
                    applicationProperties.getSecurity().getSisuHy().privateKeyLocation!!
                )
            val rsa: RSAPrivateKey =
                RsaKeyConverters.pkcs8().convert(privateKeyResource.inputStream)!!
            val keyPair = KeyPair(certificate.publicKey, rsa)
            val certificates: HandshakeCertificates = HandshakeCertificates.Builder()
                .addPlatformTrustedCertificates()
                .heldCertificate(HeldCertificate(keyPair, certificate))
                .build()
            OkHttpClient.Builder()
                .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
                .addInterceptor(
                    OkHttp3RequestInterceptor(
                        mapOf(
                            "X-Api-Key" to applicationProperties.getSecurity().getSisuHy().apiKey!!
                        )
                    )
                )
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
        }

        val apolloClient: ApolloClient by lazy {
            ApolloClient.Builder()
                .serverUrl(applicationProperties.getSecurity().getSisuHy().graphqlEndpointUrl!!)
                .okHttpClient(okHttpClient)
                .build()
        }
    }

    override fun apolloClient(): ApolloClient {
        return apolloClient
    }

    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }
}
