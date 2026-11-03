package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistuminenArkistointiDocuments
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ValmistuminenArkistointiDocumentsTest {
    private val properties = ApplicationProperties()
    private val snapshots = mock<ArkistointiAsiakirjaSnapshotService>()
    private val service = ValmistuminenArkistointiDocuments(ArkistointiConfigurationProvider(properties), snapshots)
    private val request = mock<Valmistumispyynto>()
    private val generated = listOf(RecordType.YHTEENVETO, RecordType.LIITE).map { RecordProperties(Asiakirja(id = 1), it) }

    private fun metadata(certificate: Boolean) = ApplicationProperties.Arkistointi.Metadata().apply {
        cases = mapOf("valmistuminen" to ApplicationProperties.Arkistointi.Case().apply {
            documents = (listOf("yhteenveto", "liite") + if (certificate) listOf("laillistamistodistus") else emptyList())
                .associateWith { ApplicationProperties.Arkistointi.DocumentMetadata() }
        })
    }

    @ParameterizedTest
    @EnumSource(value = YliopistoEnum::class, names = ["TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO"])
    fun `certificate selection is configured independently for each university`(university: YliopistoEnum) {
        val turku = properties.getArkistointi().getTurku()
        val uef = properties.getArkistointi().getUef()
        turku.metadata = metadata(university != YliopistoEnum.TURUN_YLIOPISTO)
        uef.metadata = metadata(university != YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        assertThat(service.complete(request, university, generated)).isSameAs(generated)
        verifyNoInteractions(request, snapshots)

        if (university == YliopistoEnum.TURUN_YLIOPISTO) turku.metadata = metadata(true) else uef.metadata = metadata(true)
        val studyRight = mock<Opintooikeus>()
        whenever(request.opintooikeus).thenReturn(studyRight)
        whenever(studyRight.erikoistuvaLaakari).thenReturn(mock<ErikoistuvaLaakari>())
        val snapshot = Asiakirja(id = 44, archiveSnapshot = true)
        whenever(snapshots.createSnapshot(any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(ArkistointiAsiakirjaReference(snapshot, RecordType.LAILLISTAMISTODISTUS, 2, "certificate.pdf", "application/pdf", "sha"))
        val result = service.complete(request, university, generated)
        assertThat(result.map { it.type }).containsExactly(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
        assertThat(result.last().asiakirja).isSameAs(snapshot)
    }

    @ParameterizedTest
    @EnumSource(value = YliopistoEnum::class, names = ["TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO"])
    fun `missing configured generated document fails before snapshot creation`(university: YliopistoEnum) {
        properties.getArkistointi().getTurku().metadata = metadata(true)
        properties.getArkistointi().getUef().metadata = metadata(true)
        assertThatThrownBy { service.complete(request, university, generated.take(1)) }
            .hasMessage("Configured graduation document is missing")
        verifyNoInteractions(request, snapshots)
    }
}
