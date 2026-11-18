package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ArkistoinninKellonKayttoTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `arkistokopion lisaysaika tulee kellosta ja alkuperaiset tavut sailyvat`(aika: KellonTestitapaus) {
        val asiakirjat = mock<AsiakirjaRepository>()
        whenever(asiakirjat.save(any<Asiakirja>())).thenAnswer { (it.arguments[0] as Asiakirja).apply { id = 1L } }
        val sisalto = byteArrayOf(1, 2, 3)
        val tulos = ArkistointiAsiakirjaSnapshotService(asiakirjat, aika.kello())
            .createSnapshot(Opintooikeus(id = 11L), "todistus.pdf", "application/pdf", sisalto, RecordType.LIITE, 0)

        assertThat(tulos.asiakirja.lisattypvm).isEqualTo(aika.paikallinenAika())
        assertThat(tulos.asiakirja.asiakirjaData!!.data).isEqualTo(sisalto)
        assertThat(tulos.asiakirja.archiveSnapshot).isTrue()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `valmistuneen arkistoinnin aikaleima tulee kellosta eika muutu uusintakutsussa`(aika: KellonTestitapaus) {
        val tehtavat = mock<ArkistointiJobRepository>()
        val lukko = mock<ArkistointiExecutionLock>()
        val tunniste = UUID.fromString("c03b83ac-48eb-4aa1-9332-ea090156556b")
        val tehtava = ArkistointiJob(id = tunniste, university = YliopistoEnum.TURUN_YLIOPISTO, caseType = CaseType.KOEJAKSO)
        whenever(tehtavat.findByIdForStateChange(tunniste)).thenReturn(tehtava)
        val suoritus = ArkistointiJobExecution("arkistointi", tunniste.toString(), 1L)
        val palvelu = ArkistointiJobStateService(tehtavat, jacksonObjectMapper(), lukko, mock(), aika.kello())

        palvelu.succeed(suoritus, "arkisto-1")
        palvelu.succeed(suoritus, "arkisto-2")

        assertThat(tehtava.completedAt).isEqualTo(aika.aikaleima())
        assertThat(tehtava.externalId).isEqualTo("arkisto-1")
        assertThat(tehtava.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
    }
}
