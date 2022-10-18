package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.ElsaBackendApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ElsaBackendApp::class])
class LocalDateParseTest {

    @ParameterizedTest
    @ValueSource(strings = ["2022-01-01", "1994-05-25T00:00:00"])
    fun testLocalDate(input: String) {
        assertThat(input.tryParseToLocalDate()).isNotNull
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-date"])
    fun testLocalDateError(input: String) {
        assertThat(input.tryParseToLocalDate()).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["1994-05-25T00:00:00"])
    fun testLocalDateTime(input: String) {
        assertThat(input.tryParseToLocalDateTime()).isNotNull
    }

}
