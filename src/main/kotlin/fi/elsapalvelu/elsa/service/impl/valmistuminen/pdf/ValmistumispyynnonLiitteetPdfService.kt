package fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.DocumentContentTypes
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistumispyynnonAsiakirjanTallennusService
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.http.MediaType
import java.io.ByteArrayOutputStream

@Service
class ValmistumispyynnonLiitteetPdfService(
    private val pdfService: PdfService,
    private val asiakirjanTallennusService: ValmistumispyynnonAsiakirjanTallennusService,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val snapshotService: ArkistointiAsiakirjaSnapshotService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun luo(valmistumispyynto: Valmistumispyynto, liitettavat: List<Asiakirja>): Asiakirja {
        val yhdistelynAlku = System.currentTimeMillis()
        val outputStream = ByteArrayOutputStream()
        pdfService.yhdistaAsiakirjat(
            liitettavat,
            outputStream
        )
        val data = outputStream.toByteArray()
        log.info(
            "Liitteet yhdistetty [asiakirjoja=${liitettavat.size}, " +
                "koko=${data.size / MIB} MiB, " +
                "kesto=${System.currentTimeMillis() - yhdistelynAlku} ms]"
        )

        val tallennuksenAlku = System.currentTimeMillis()
        val asiakirja = asiakirjanTallennusService.tallenna(
            valmistumispyynto,
            "valmistumisen_yhteenvedon_liitteet",
            data
        ) { pyynto, tallennettu -> pyynto.liitteetAsiakirja = tallennettu }
        log.info(
            "Liitteet tallennettu [kesto=${System.currentTimeMillis() - tallennuksenAlku} ms]"
        )
        return asiakirja
    }

    fun createArchiveReference(asiakirja: Asiakirja, sources: List<Asiakirja>, jarjestysnumero: Int) =
        snapshotService.createMergedReference(
            asiakirja,
            // Ordinary merging writes PDFs first, then images; preserve that order in the archive copy.
            sources.sortedBy {
                if (DocumentContentTypes.canonical(it.tyyppi) == MediaType.APPLICATION_PDF_VALUE) 0 else 1
            },
            RecordType.LIITE,
            jarjestysnumero
        )

    /** Shared selection for the ordinary PDF and the archival copy, within the approval transaction. */
    fun loadSourceDocuments(valmistumispyynto: Valmistumispyynto): List<Asiakirja> =
        tyoskentelyjaksoRepository.findAllByOpintooikeusId(valmistumispyynto.opintooikeus.required().id.required())
            .flatMap { it.asiakirjat }

    private companion object {
        const val MIB = 1024 * 1024
    }
}
