package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.ValmistumispyynnonErikoistujanTiedotPdfService
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.ValmistumispyynnonLiitteetPdfService
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.ValmistumispyynnonYhteenvetoPdfService
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ValmistumisLiitteetArchiveTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `generation merges selected attachments once and returns the ready archive documents`(yek: Boolean) {
        val periods = mock<TyoskentelyjaksoRepository>()
        val pdf = mock<PdfService>()
        val storage = mock<ValmistumispyynnonAsiakirjanTallennusService>()
        val summary = mock<ValmistumispyynnonYhteenvetoPdfService>()
        val sources = listOf(
            Asiakirja(id = 1L, tyyppi = "image/png"),
            Asiakirja(id = 2L, tyyppi = "application/pdf"),
            Asiakirja(id = 3L, tyyppi = "image/jpg")
        )
        val ordinary = Asiakirja(id = 4L)
        val studyRight = Opintooikeus(id = 10L, erikoisala = Erikoisala(id = if (yek) YEK_ERIKOISALA_ID else -1L))
        val request = Valmistumispyynto(opintooikeus = studyRight)
        val attachments = ValmistumispyynnonLiitteetPdfService(pdf, storage, periods)
        val creator = ValmistumispyynnonAsiakirjojenLuontiService(summary, attachments, mock<ValmistumispyynnonErikoistujanTiedotPdfService>())
        whenever(periods.findAllByOpintooikeusId(10L)).thenReturn(
            listOf(Tyoskentelyjakso(asiakirjat = sources.toMutableSet())), emptyList()
        )
        whenever(storage.tallenna(any(), any(), any(), any())).thenReturn(ordinary)
        if (yek) {
            whenever(summary.luoYek(any(), any())).thenReturn(Asiakirja(id = 5L))
        } else {
            whenever(summary.luo(any(), any())).thenReturn(Asiakirja(id = 5L))
        }
        val created = creator.luo(ValmistumispyynnonTarkistusDTO(), request)
        assertThat(created.map { it.type }).containsExactly(RecordType.YHTEENVETO, RecordType.LIITE)
        assertThat(created.single { it.type == RecordType.LIITE }.asiakirja).isSameAs(ordinary)
        verify(pdf).yhdistaAsiakirjat(eq(sources), any())
        // There is no second archive assembly that could observe a different source selection.
        verify(periods, times(1)).findAllByOpintooikeusId(10L)
    }
}
