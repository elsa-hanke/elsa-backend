package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ArkistointiManifestValidatorTest {
    private val identities = listOf(
        ArkistointiDocumentIdentity(10, 20, RecordType.YHTEENVETO, 0),
        ArkistointiDocumentIdentity(11, 21, RecordType.LIITE, 1)
    )
    private val documents = identities.map {
        LoadedArkistointiAsiakirja(it.snapshotDocumentId, it.recordType, it.jarjestysnumero,
            "document.pdf", "application/pdf", "checksum", byteArrayOf(1))
    }

    @Test
    fun `matches by snapshot and preserves source identities regardless of list order`() {
        val result = ArkistointiManifestValidator.validate(identities.reversed(), documents)
        assertThat(result.getValue(20).sourceDocumentId).isEqualTo(10)
        assertThat(result.getValue(21).sourceDocumentId).isEqualTo(11)
        assertThat(ArkistointiManifestValidator.validate(identities, documents.reversed())).isEqualTo(result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["empty", "count", "source", "snapshot", "order", "duplicateSnapshot",
        "duplicateOrder", "duplicateLoaded", "unknownSnapshot", "type", "mismatchedOrder"])
    fun `rejects inconsistent manifest`(kind: String) {
        var saved = identities
        var loaded = documents
        when (kind) {
            "empty" -> { saved = emptyList(); loaded = emptyList() }
            "count" -> loaded = documents.take(1)
            "source" -> saved = listOf(identities[0].copy(sourceDocumentId = 0), identities[1])
            "snapshot" -> saved = listOf(identities[0].copy(snapshotDocumentId = 0), identities[1])
            "order" -> saved = listOf(identities[0].copy(jarjestysnumero = -1), identities[1])
            "duplicateSnapshot" -> saved = listOf(identities[0], identities[1].copy(snapshotDocumentId = 20))
            "duplicateOrder" -> saved = listOf(identities[0], identities[1].copy(jarjestysnumero = 0))
            "duplicateLoaded" -> loaded = listOf(documents[0], documents[0])
            "unknownSnapshot" -> loaded = listOf(documents[0].copy(asiakirjaId = 99), documents[1])
            "type" -> loaded = listOf(documents[0].copy(asiakirjatyyppi = RecordType.LIITE), documents[1])
            "mismatchedOrder" -> loaded = listOf(documents[0].copy(jarjestysnumero = 2), documents[1])
        }
        assertThatThrownBy { ArkistointiManifestValidator.validate(saved, loaded) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
