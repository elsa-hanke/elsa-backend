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
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistuminenArkistointiAsiakirjat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ValmistuminenArkistointiAsiakirjatTest {
    private val properties = ApplicationProperties()
    private val snapshots = mock<ArkistointiAsiakirjaSnapshotService>()
    private val configuration = ArkistointiConfigurationProvider(properties)
    private val service = ValmistuminenArkistointiAsiakirjat(snapshots)
    private val valmistumispyynto = mock<Valmistumispyynto>()
    private val generated = listOf(RecordType.YHTEENVETO, RecordType.LIITE).map { RecordProperties(Asiakirja(id = 1), it) }

    @Test
    fun `certificate is skipped when the university archives only generated documents`() {
        properties.getArkistointi().getHki().metadata = ApplicationProperties.Arkistointi.Metadata().apply {
            cases = mapOf("valmistuminen" to ApplicationProperties.Arkistointi.Case().apply {
                documents = listOf("yhteenveto", "liite").associateWith { ApplicationProperties.Arkistointi.DocumentMetadata() }
            })
        }
        val settings = configuration.getConfiguration(YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.VALMISTUMINEN)
        assertThat(service.taydenna(valmistumispyynto, settings, generated)).isSameAs(generated)
        verifyNoInteractions(valmistumispyynto, snapshots)
    }

    @ParameterizedTest
    @EnumSource(value = YliopistoEnum::class, names = ["TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO"])
    fun `Dynasty graduation includes the certificate from the university profile`(university: YliopistoEnum) {
        val opintooikeus = mock<Opintooikeus>()
        whenever(valmistumispyynto.opintooikeus).thenReturn(opintooikeus)
        whenever(opintooikeus.erikoistuvaLaakari).thenReturn(mock<ErikoistuvaLaakari>())
        val snapshot = Asiakirja(id = 44, archiveSnapshot = true)
        whenever(snapshots.createSnapshot(any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(ArkistointiAsiakirjaReference(snapshot, RecordType.LAILLISTAMISTODISTUS, 2, "certificate.pdf", "application/pdf", "sha"))
        val settings = configuration.getConfiguration(university, CaseType.VALMISTUMINEN)
        val result = service.taydenna(valmistumispyynto, settings, generated)
        assertThat(result.map { it.type }).containsExactly(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
        assertThat(result.last().asiakirja).isSameAs(snapshot)
    }

    @ParameterizedTest
    @EnumSource(value = YliopistoEnum::class, names = ["TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO"])
    fun `missing configured generated document fails before snapshot creation`(university: YliopistoEnum) {
        val settings = configuration.getConfiguration(university, CaseType.VALMISTUMINEN)
        assertThatThrownBy { service.taydenna(valmistumispyynto, settings, generated.take(1)) }
            .hasMessage("Configured graduation document is missing")
        verifyNoInteractions(valmistumispyynto, snapshots)
    }
}
