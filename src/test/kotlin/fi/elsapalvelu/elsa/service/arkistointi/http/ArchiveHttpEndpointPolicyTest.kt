package fi.elsapalvelu.elsa.service.arkistointi.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ArchiveHttpEndpointPolicyTest {
    @ParameterizedTest
    @CsvSource(value = [
        "https|archive.example|false|true",
        "https|archive.example|true|true",
        "http|127.0.0.1|false|false",
        "http|::1|false|false",
        "http|127.0.0.1|true|true",
        "http|::1|true|true",
        "http|[::1]|true|true",
        "http|localhost|true|false",
        "http|127.0.0.1.example|true|false",
        "http|192.0.2.1|true|false",
        "http|::ffff:127.0.0.1|true|false",
        "ftp|127.0.0.1|true|false",
        "https||true|false",
        "|127.0.0.1|true|false"
    ], delimiter = '|')
    fun `HTTPS required except for explicit literal loopback HTTP tests`(
        scheme: String?, host: String?, allowTestHttp: Boolean, expected: Boolean
    ) {
        assertThat(ArchiveHttpEndpointPolicy.isAllowed(scheme, host, allowTestHttp)).isEqualTo(expected)
    }
}
