package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaSnapshotService
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
    fun `ordinary and archive PDFs use one attachment selection with the same PDF-first order`(yek: Boolean) {
        val periods = mock<TyoskentelyjaksoRepository>()
        val pdf = mock<PdfService>()
        val storage = mock<ValmistumispyynnonAsiakirjanTallennusService>()
        val snapshots = mock<ArkistointiAsiakirjaSnapshotService>()
        val summary = mock<ValmistumispyynnonYhteenvetoPdfService>()
        val sources = listOf(
            Asiakirja(id = 1L, tyyppi = "image/png"),
            Asiakirja(id = 2L, tyyppi = "application/pdf"),
            Asiakirja(id = 3L, tyyppi = "image/jpg")
        )
        val ordinary = Asiakirja(id = 4L)
        val studyRight = Opintooikeus(id = 10L, erikoisala = Erikoisala(id = if (yek) YEK_ERIKOISALA_ID else -1L))
        val request = Valmistumispyynto(opintooikeus = studyRight)
        val attachments = ValmistumispyynnonLiitteetPdfService(pdf, storage, periods, snapshots)
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
        val reference = ArkistointiAsiakirjaReference(Asiakirja(id = 6L), RecordType.LIITE, 1, "liitteet.pdf", "application/pdf", "hash")
        whenever(snapshots.createMergedReference(any(), any(), any(), any())).thenReturn(reference)

        val created = creator.luo(ValmistumispyynnonTarkistusDTO(), request)
        val actual = attachments.createArchiveReference(
            created.records.single { it.type == RecordType.LIITE }.asiakirja, created.attachmentSources, 1
        )

        assertThat(actual).isSameAs(reference)
        assertThat(created.attachmentSources).containsExactlyElementsOf(sources)
        verify(pdf).yhdistaAsiakirjat(eq(sources), any())
        verify(snapshots).createMergedReference(ordinary, listOf(sources[1], sources[0], sources[2]), RecordType.LIITE, 1)
        // A later query would see a different set; archive assembly must use the originally selected sources.
        verify(periods, times(1)).findAllByOpintooikeusId(10L)
    }
}
