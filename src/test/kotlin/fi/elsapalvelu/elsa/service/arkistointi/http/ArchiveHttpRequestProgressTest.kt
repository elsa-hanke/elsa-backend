package fi.elsapalvelu.elsa.service.arkistointi.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.ConnectException
import javax.net.ssl.SSLHandshakeException

class ArchiveHttpRequestProgressTest {
    @Test
    fun `TLS failure suppressed by another route's connection failure is not retryable`() {
        val failure = ConnectException().apply { addSuppressed(SSLHandshakeException("untrusted certificate")) }

        assertThat(ArchiveHttpRequestProgress().failure(failure)).isEqualTo(ArchiveHttpFailure.TLS_FAILED)
    }
}
