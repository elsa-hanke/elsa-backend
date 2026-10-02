package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ValmistumispyynnonLiitteetPdfService(
    private val pdfService: PdfService,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
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
        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))
        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = opintooikeus,
                nimi = "valmistumisen_yhteenvedon_liitteet_${timestamp}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = data)
            )
        )
        valmistumispyynto.liitteetAsiakirja = asiakirja
        valmistumispyyntoRepository.save(valmistumispyynto)
        log.info(
            "Liitteet tallennettu [kesto=${System.currentTimeMillis() - tallennuksenAlku} ms]"
        )
        return asiakirja
    }

    private companion object {
        const val DATE_FORMAT = "yyyyMMdd"
        const val MIB = 1024 * 1024
    }
}
