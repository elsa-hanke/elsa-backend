package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.seuranta.SeurantajaksoService
import fi.elsapalvelu.elsa.service.seuranta.SeurantajaksoPdfTextValidator
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentException
import fi.elsapalvelu.elsa.web.rest.errors.InvalidPdfAttachmentSource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class ValmistumispyynnonErikoistujanTiedotPdfService(
    private val pdfService: PdfService,
    private val asiakirjaRepository: AsiakirjaRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository,
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val seurantajaksoService: SeurantajaksoService,
    private val arviointiPdfService: ValmistumispyynnonArviointiPdfService,
    private val suoritemerkintaPdfService: ValmistumispyynnonSuoritemerkintaPdfService,
    private val pdfContentValidator: PdfContentValidator,
    private val seurantajaksoPdfTextValidator: SeurantajaksoPdfTextValidator
) {
    fun luo(valmistumispyynto: Valmistumispyynto) {
        val opintooikeus = valmistumispyynto.opintooikeus ?: return
        val opintooikeusId = opintooikeus.id.required()
        val outputStream = ByteArrayOutputStream()

        lisaaKoulutussuunnitelma(opintooikeusId, outputStream)
        arviointiPdfService.lisaa(opintooikeusId, valmistumispyynto, outputStream)
        suoritemerkintaPdfService.lisaa(opintooikeusId, valmistumispyynto, outputStream)
        lisaaPaivakirjamerkinnat(opintooikeusId, outputStream)
        lisaaSeurantajaksot(opintooikeusId, valmistumispyynto, outputStream)

        val aikaleima =
            LocalDate.now().format(DateTimeFormatter.ofPattern(PAIVAMAARAFORMAATTI))
        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = opintooikeus,
                nimi = "koulutussuunnitelma_ja_osaaminen_${aikaleima}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = outputStream.toByteArray())
            )
        )
        valmistumispyynto.erikoistujanTiedotAsiakirja = asiakirja
        valmistumispyyntoRepository.save(valmistumispyynto)
    }

    private fun lisaaKoulutussuunnitelma(
        opintooikeusId: Long,
        outputStream: ByteArrayOutputStream
    ) {
        val koulutussuunnitelma =
            koulutussuunnitelmaRepository.findOneByOpintooikeusId(opintooikeusId)
        pdfService.luoPdf(
            "pdf/erikoistujantiedot/koulutussuunnitelma.html",
            Context(SUOMEN_LOCALE).apply {
                setVariable("koulutussuunnitelma", koulutussuunnitelma)
            },
            outputStream
        )

        val motivaatiokirje = koulutussuunnitelma?.motivaatiokirjeAsiakirja ?: return
        val data = motivaatiokirje.asiakirjaData?.data
        if (
            motivaatiokirje.tyyppi != MediaType.APPLICATION_PDF_VALUE ||
            data == null ||
            !pdfContentValidator.isValid(data)
        ) {
            throw InvalidPdfAttachmentException(
                attachmentId = motivaatiokirje.id,
                attachmentName = motivaatiokirje.nimi,
                source = InvalidPdfAttachmentSource.MOTIVAATIOKIRJE
            )
        }
        val existingPdf = ByteArrayInputStream(outputStream.toByteArray())
        outputStream.reset()
        try {
            pdfService.yhdistaPdf(existingPdf, ByteArrayInputStream(data), outputStream)
        } catch (e: Exception) {
            throw InvalidPdfAttachmentException(
                attachmentId = motivaatiokirje.id,
                attachmentName = motivaatiokirje.nimi,
                source = InvalidPdfAttachmentSource.MOTIVAATIOKIRJE,
                cause = e
            )
        }
    }

    private fun lisaaPaivakirjamerkinnat(
        opintooikeusId: Long,
        outputStream: ByteArrayOutputStream
    ) {
        val paivakirjamerkinnat =
            paivakirjamerkintaRepository.findAllByOpintooikeusId(opintooikeusId)
        val paivakirjamerkinnatStream = ByteArrayOutputStream()
        pdfService.luoPdf(
            "pdf/erikoistujantiedot/paivittaisetmerkinnat.html",
            Context(SUOMEN_LOCALE).apply {
                setVariable("paivakirjamerkinnat", paivakirjamerkinnat)
            },
            paivakirjamerkinnatStream
        )
        lisaaPdf(paivakirjamerkinnatStream, outputStream)
    }

    private fun lisaaSeurantajaksot(
        opintooikeusId: Long,
        valmistumispyynto: Valmistumispyynto,
        outputStream: ByteArrayOutputStream
    ) {
        val arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko
        val arviointiasteikonTasot = arviointiasteikko?.tasot?.associateBy { it.taso }
        seurantajaksoService.findByOpintooikeusId(opintooikeusId).forEach { seurantajakso ->
            seurantajaksoPdfTextValidator.validateKaikkiKentat(seurantajakso)
            val seurantajaksonTiedot = seurantajaksoService.findSeurantajaksonTiedot(
                seurantajakso.opintooikeusId.required(),
                seurantajakso.alkamispaiva.required(),
                seurantajakso.paattymispaiva.required(),
                seurantajakso.koulutusjaksot?.map { it.id.required() }.orEmpty()
            )
            val seurantajaksoStream = ByteArrayOutputStream()
            pdfService.luoPdf(
                "pdf/erikoistujantiedot/seurantajakso.html",
                Context(SUOMEN_LOCALE).apply {
                    setVariable("seurantajakso", seurantajakso)
                    setVariable("seurantajaksonTiedot", seurantajaksonTiedot)
                    setVariable("arviointiasteikko", arviointiasteikko)
                    setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
                },
                seurantajaksoStream
            )
            lisaaPdf(seurantajaksoStream, outputStream)
        }
    }

    private fun lisaaPdf(
        newDocument: ByteArrayOutputStream,
        outputStream: ByteArrayOutputStream
    ) {
        val existingPdf = ByteArrayInputStream(outputStream.toByteArray())
        val newPdf = ByteArrayInputStream(newDocument.toByteArray())
        outputStream.reset()
        pdfService.yhdistaPdf(existingPdf, newPdf, outputStream)
    }

    private companion object {
        const val PAIVAMAARAFORMAATTI = "yyyyMMdd"
        val SUOMEN_LOCALE: Locale = Locale.forLanguageTag("fi")
    }
}
