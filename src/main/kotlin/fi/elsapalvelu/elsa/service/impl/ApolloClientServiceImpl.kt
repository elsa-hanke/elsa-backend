package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.ApolloClientService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.opensaml.security.x509.X509Support
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.stereotype.Service
import java.security.KeyPair
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey

@Service
class ApolloClientServiceImpl(
    private val resourceLoader: ResourceLoader,
    private val applicationProperties: ApplicationProperties
) : ApolloClientService {
    override fun getSisuHyApolloClient(): ApolloClient {
        val publicKeyResource: Resource =
            resourceLoader.getResource(
                applicationProperties.getSecurity().getSisuHy().certificateLocation!!
            )
        val certificate: X509Certificate =
            X509Support.decodeCertificate(publicKeyResource.inputStream.readBytes())!!
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
        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            .addInterceptor(ApiKeyInterceptor(applicationProperties.getSecurity().getSisuHy().apiKey!!))
            .build()

        return ApolloClient.Builder()
            .serverUrl(applicationProperties.getSecurity().getSisuHy().graphQlEndpointUrl!!)
            .okHttpClient(okHttpClient)
            .build()
    }
}

private class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Api-Key", apiKey)
            .build()

        return chain.proceed(request)
    }
}
