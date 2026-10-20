package fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistumispyynnonAsiakirjanTallennusService
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class ValmistumispyynnonLiitteetPdfService(
    private val pdfService: PdfService,
    private val asiakirjanTallennusService: ValmistumispyynnonAsiakirjanTallennusService,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun luo(valmistumispyynto: Valmistumispyynto): Asiakirja {
        val opintooikeus = valmistumispyynto.opintooikeus.required()
        val tyoskentelyjaksot = tyoskentelyjaksoRepository.findAllByOpintooikeusId(
            opintooikeus.id.required()
        )
        val liitettavat = tyoskentelyjaksot.flatMap { it.asiakirjat }
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

    private companion object {
        const val MIB = 1024 * 1024
    }
}
