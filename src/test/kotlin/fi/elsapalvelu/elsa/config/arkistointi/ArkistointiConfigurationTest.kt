package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.config.ApplicationProperties.Arkistointi
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.LIITE
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.SOPIMUS
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.YHTEENVETO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ArkistointiConfigurationTest {
    @Test
    fun `document metadata is looked up for the configured type`() {
        val metadata = Arkistointi.DocumentMetadata()
        val configuration = configuration(mapOf("yhteenveto" to metadata))

        assertThat(configuration.getDocumentMetadata(YHTEENVETO)).isSameAs(metadata)
        assertThat(configuration.includesDocument(YHTEENVETO)).isTrue()
        assertThat(configuration.getDocumentMetadata(SOPIMUS)).isNull()
        assertThat(configuration.includesDocument(SOPIMUS)).isFalse()
    }

    @Test
    fun `selection keeps input order and multiple records of the same type`() {
        val configuration = configuration(mapOf(
            "yhteenveto" to Arkistointi.DocumentMetadata(),
            "liite" to Arkistointi.DocumentMetadata()
        ))
        val firstAttachment = RecordProperties(Asiakirja(id = 1), LIITE)
        val contract = RecordProperties(Asiakirja(id = 2), SOPIMUS)
        val summary = RecordProperties(Asiakirja(id = 3), YHTEENVETO)
        val secondAttachment = RecordProperties(Asiakirja(id = 4), LIITE)

        val selected = configuration.selectDocuments(listOf(firstAttachment, contract, summary, secondAttachment))

        assertThat(selected).containsExactly(firstAttachment, summary, secondAttachment)
    }

    @Test
    fun `completeness requires every configured type and permits extra types`() {
        val configuration = configuration(mapOf(
            "yhteenveto" to Arkistointi.DocumentMetadata(),
            "liite" to Arkistointi.DocumentMetadata()
        ))

        assertThat(configuration.hasAllRequiredDocuments(listOf(LIITE, SOPIMUS, YHTEENVETO))).isTrue()
        assertThat(configuration.hasAllRequiredDocuments(listOf(YHTEENVETO, YHTEENVETO, SOPIMUS))).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["unknown", "YHTEENVETO"])
    fun `completeness rejects a configured key that does not match a record type`(key: String) {
        val configuration = configuration(mapOf("yhteenveto" to Arkistointi.DocumentMetadata(), key to Arkistointi.DocumentMetadata()))

        assertThat(configuration.hasAllRequiredDocuments(listOf(YHTEENVETO))).isFalse()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `absent and empty document maps impose no requirements and select no records`(configured: Boolean) {
        val configuration = configuration(if (configured) emptyMap() else null)

        assertThat(configuration.hasAllRequiredDocuments(emptyList())).isTrue()
        assertThat(configuration.selectDocuments(listOf(RecordProperties(Asiakirja(id = 1), YHTEENVETO)))).isEmpty()
    }

    private fun configuration(documents: Map<String, Arkistointi.DocumentMetadata>?): ArkistointiConfiguration =
        ArkistointiConfiguration(Arkistointi.Metadata(), Arkistointi.Case().apply { this.documents = documents })
}
