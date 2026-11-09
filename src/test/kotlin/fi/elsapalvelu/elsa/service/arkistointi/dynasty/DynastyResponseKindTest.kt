package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class DynastyResponseKindTest {
    @Test
    fun `typed identifiers preserve the remote representation when resuming a step`() {
        val caseId = DynastyResponseKind.CaseId.fromSaved("000415")
        val taskId = "8AE4F236-0CB5-4028-B4D2-C25DA9643285"
        assertThat(caseId.numericValue).isEqualTo(415)
        assertThat(DynastyResponseKind.CaseId.toSaved(caseId)).isEqualTo("000415")
        assertThat(DynastyResponseKind.TaskId.toSaved(DynastyResponseKind.TaskId.fromSaved(taskId))).isEqualTo(taskId)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["0", "-1", "+1", "1.2", "9223372036854775808", " 415", "\"415\"", "private response"])
    fun `saved numeric identifiers reject malformed or out of range values`(id: String?) {
        listOf<() -> Any>({ DynastyResponseKind.CaseId.fromSaved(id) }, { DynastyResponseKind.DocumentId.fromSaved(id) }).forEach { parse ->
            assertThatThrownBy { parse() }.isInstanceOf(DynastyFailure::class.java)
                .hasMessage("Dynasty request failed [DYNASTY_RESPONSE_INVALID]").hasNoCause()
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["1-1-1-1-1", "415", "private response", " 00000000-0000-0000-0000-000000000000"])
    fun `task identifier requires a canonical UUID`(id: String?) {
        assertThatThrownBy { DynastyResponseKind.TaskId.fromSaved(id) }
            .isInstanceOf(DynastyFailure::class.java).hasNoCause()
    }

    @Test
    fun `each step reads and persists its own result type`() {
        val task = DynastyTaskId(UUID.randomUUID())
        assertThat(DynastyStepKey.Case.responseKind.fromSaved("415")).isEqualTo(DynastyCaseId(415))
        assertThat(DynastyStepKey.Document(0).responseKind.fromSaved(Long.MAX_VALUE.toString())).isEqualTo(DynastyDocumentId(Long.MAX_VALUE))
        assertThat(DynastyStepKey.Task.responseKind.fromSaved(task.toString())).isEqualTo(task)
        assertThat(DynastyStepKey.Case.responseKind.toSaved(DynastyCaseId(415))).isEqualTo("415")
        assertThat(DynastyStepKey.Document(0).responseKind.toSaved(DynastyDocumentId(1078))).isEqualTo("1078")
        assertThat(DynastyStepKey.Task.responseKind.toSaved(task)).isEqualTo(task.toString())
        assertThat(DynastyStepKey.ArchiveCase.responseKind.fromSaved(null)).isEqualTo(Unit)
        assertThat(DynastyStepKey.ArchiveCase.responseKind.toSaved(Unit)).isNull()
    }
}
