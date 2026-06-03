package fi.elsapalvelu.elsa.config

import io.awspring.cloud.s3.S3ProtocolResolver
import io.awspring.cloud.s3.S3Resource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Guards against accidental removal of the spring-cloud-aws-starter-s3 dependency.
 *
 * [RelyingPartyConfiguration] loads SAML certificates and private keys via Spring's ResourceLoader
 * using S3 URIs (e.g. s3://bucket/samlPublicKey.crt), as configured through
 * APPLICATION_SECURITY_SUOMIFI_SAML_CERTIFICATE_LOCATION in infra-live/ecs/terragrunt.hcl.
 *
 * The s3:// protocol is only understood by Spring's ResourceLoader when [S3ProtocolResolver]
 * is on the classpath, which is provided exclusively by the spring-cloud-aws-starter-s3 dependency.
 * Removing it would cause application startup failures in every environment where SAML is enabled.
 */
class SpringCloudAwsS3DependencyTest {

    /**
     * Compile-time guard: this file will not compile if spring-cloud-aws-starter-s3 is removed,
     * because the [S3Resource] import will be unresolvable.
     */
    @Test
    fun `S3Resource class is present on the classpath`() {
        val s3ResourceClass: Class<S3Resource> = S3Resource::class.java
        assertThat(s3ResourceClass)
            .`as`(
                "io.awspring.cloud:spring-cloud-aws-starter-s3 must remain a dependency. " +
                    "RelyingPartyConfiguration loads SAML certificates from S3 URIs " +
                    "(e.g. s3://bucket/samlPublicKey.crt) via Spring's ResourceLoader. " +
                    "The s3:// protocol is only supported when S3ProtocolResolver is on the classpath."
            )
            .isNotNull
    }

    /**
     * Compile-time guard: this file will not compile if spring-cloud-aws-starter-s3 is removed,
     * because the [S3ProtocolResolver] import will be unresolvable.
     */
    @Test
    fun `S3ProtocolResolver is present on the classpath`() {
        val resolverClass: Class<S3ProtocolResolver> = S3ProtocolResolver::class.java
        assertThat(resolverClass)
            .`as`(
                "S3ProtocolResolver must be on the classpath. " +
                    "It is provided by io.awspring.cloud:spring-cloud-aws-starter-s3 and is " +
                    "required so that Spring's ResourceLoader can resolve s3:// URIs used by " +
                    "RelyingPartyConfiguration to load SAML keys and certificates at runtime."
            )
            .isNotNull
    }
}

