package fi.elsapalvelu.elsa.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class RichTextContractTest {
    @Test
    fun `shared security corpus preserves permitted formatting and rejects executable markup`() {
        val cases = ClassPathResource("security/rich-text-cases.json").inputStream.use {
            ObjectMapper().readTree(it)
        }
        cases.forEach { case ->
            val sanitized = RichTextHtmlSanitizer.sanitize(case["html"].asText())!!
            assertThat(sanitized).describedAs(case["name"].asText())
                .contains(case["text"].asText())
                .doesNotContain("<script", "<iframe", "<img", "<svg", "<math", "<form", "<input", "style=", "onclick=", "onerror=")
            assertThat(sanitized).doesNotMatch("(?is).*href=\"\\s*(javascript|vbscript|data):.*")
            if (case["blockedLink"]?.asBoolean() == true) assertThat(sanitized).doesNotContain("href=")
            case["contains"].forEach { assertThat(sanitized).contains(it.asText()) }
            if (case["name"].asText() == "invalid-table-attrs") {
                assertThat(sanitized).doesNotContain("colspan=", "rowspan=", "scope=", "start=")
            }
            assertThat(RichTextHtmlSanitizer.sanitize(sanitized)).isEqualTo(sanitized)
        }
    }
}
