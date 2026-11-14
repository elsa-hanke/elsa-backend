package fi.elsapalvelu.elsa.service.arkistointi.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArchiveLogExceptionTest {
    @Test
    fun `logging preserves exception types and frames without private messages`() {
        val cause = java.io.IOException("private document.pdf")
        val failure = IllegalStateException("private payload", cause)
        val suppressed = IllegalArgumentException("private credentials")
        failure.addSuppressed(suppressed)
        val logged = archiveLogException(failure)
        assertThat(logged.stackTraceToString()).doesNotContain("private document.pdf", "private payload", "private credentials")
            .contains(failure.javaClass.name, cause.javaClass.name, suppressed.javaClass.name)
        assertThat(logged.stackTrace).isEqualTo(failure.stackTrace)
        assertThat(logged.cause!!.stackTrace).isEqualTo(cause.stackTrace)
        assertThat(logged.suppressed.single().stackTrace).isEqualTo(suppressed.stackTrace)
        assertThat(failure.message).isEqualTo("private payload")
    }

    @Test
    fun `cyclic exception graphs are bounded`() {
        val first = IllegalStateException("private first")
        val second = IllegalArgumentException("private second", first)
        first.initCause(second)
        first.addSuppressed(second)
        val logged = archiveLogException(first)
        assertThat(logged.stackTraceToString()).doesNotContain("private")
        assertThat(logged.cause!!.cause).isNull()
    }
}
