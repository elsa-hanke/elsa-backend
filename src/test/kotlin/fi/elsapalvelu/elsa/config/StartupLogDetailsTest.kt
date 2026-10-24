package fi.elsapalvelu.elsa.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class StartupLogDetailsTest {
    @Test
    fun extractsDatabaseAddressWithoutCredentialsOrQueryParameters() {
        val address = StartupLogDetails.databaseAddress(
            "jdbc:postgresql://user:secret@db.example:15432/elsaBackend?password=secret&sslmode=require"
        )

        assertThat(address).isEqualTo("host=db.example, port=15432, database=elsaBackend")
    }

    @Test
    fun usesDefaultPostgresPort() {
        assertThat(StartupLogDetails.databaseAddress("jdbc:postgresql://localhost/elsaBackend"))
            .isEqualTo("host=localhost, port=5432, database=elsaBackend")
    }

    @Test
    fun supportsIpv6Addresses() {
        assertThat(StartupLogDetails.databaseAddress("jdbc:postgresql://[::1]:5432/elsaBackend"))
            .isEqualTo("host=[::1], port=5432, database=elsaBackend")
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = [
        "",
        "jdbc:mysql://user:secret@localhost/database",
        "jdbc:postgresql:elsaBackend",
        "jdbc:postgresql://localhost",
        "jdbc:postgresql://bad host/elsaBackend?password=secret",
        "jdbc:postgresql://localhost:invalid/elsaBackend",
        "jdbc:postgresql://localhost:65536/elsaBackend",
        "jdbc:postgresql://localhost/elsaBackend;password=secret",
        "jdbc:postgresql://localhost/elsaBackend/secret",
        "jdbc:postgresql://localhost/elsaBackend%0Asecret",
        "jdbc:postgresql://localhost/elsaBackend#secret",
        "jdbc:postgresql://host1:5432,host2:5432/elsaBackend?password=secret"
    ])
    fun unavailableForMissingMalformedOrUnsupportedUrls(jdbcUrl: String?) {
        assertThat(StartupLogDetails.databaseAddress(jdbcUrl)).isEqualTo("unavailable")
    }
}
