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
import org.slf4j.LoggerFactory
import org.thymeleaf.context.Context
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
    private val log = LoggerFactory.getLogger(javaClass)

    fun luo(valmistumispyynto: Valmistumispyynto) {
        val opintooikeus = valmistumispyynto.opintooikeus ?: return
        val opintooikeusId = opintooikeus.id.required()
        val aloitettu = System.currentTimeMillis()

        val data = luoKooste(opintooikeusId, valmistumispyynto).use { assembler ->
            arviointiPdfService.lisaa(opintooikeusId, valmistumispyynto, assembler)
            suoritemerkintaPdfService.lisaa(opintooikeusId, valmistumispyynto, assembler)
            lisaaPaivakirjamerkinnat(opintooikeusId, assembler)
            lisaaSeurantajaksot(opintooikeusId, valmistumispyynto, assembler)
            log.info(
                "Erikoistujan tiedot koottu [opintooikeusId=$opintooikeusId, " +
                    "sivuja=${assembler.pages}, kesto=${System.currentTimeMillis() - aloitettu} ms]"
            )
            val kirjoituksenAlku = System.currentTimeMillis()
            val tavut = assembler.finish()
            log.info(
                "Erikoistujan tiedot kirjoitettu [opintooikeusId=$opintooikeusId, " +
                    "koko=${tavut.size / MIB} MiB, " +
                    "kesto=${System.currentTimeMillis() - kirjoituksenAlku} ms]"
            )
            tavut
        }

        val tallennuksenAlku = System.currentTimeMillis()
        val aikaleima =
            LocalDate.now().format(DateTimeFormatter.ofPattern(PAIVAMAARAFORMAATTI))
        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = opintooikeus,
                nimi = "koulutussuunnitelma_ja_osaaminen_${aikaleima}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = data)
            )
        )
        valmistumispyynto.erikoistujanTiedotAsiakirja = asiakirja
        valmistumispyyntoRepository.save(valmistumispyynto)
        log.info(
            "Erikoistujan tiedot tallennettu [opintooikeusId=$opintooikeusId, " +
                "kesto=${System.currentTimeMillis() - tallennuksenAlku} ms]"
        )
    }

    private fun luoKooste(
        opintooikeusId: Long,
        @Suppress("UNUSED_PARAMETER") valmistumispyynto: Valmistumispyynto
    ): PdfAssembler {
        val koulutussuunnitelma =
            koulutussuunnitelmaRepository.findOneByOpintooikeusId(opintooikeusId)
        val koulutussuunnitelmaStream = ByteArrayOutputStream()
        pdfService.luoPdf(
            "pdf/erikoistujantiedot/koulutussuunnitelma.html",
            Context(SUOMEN_LOCALE).apply {
                setVariable("koulutussuunnitelma", koulutussuunnitelma)
            },
            koulutussuunnitelmaStream
        )
        val assembler = pdfService.openAssembler(koulutussuunnitelmaStream.toByteArray())

        val motivaatiokirje = koulutussuunnitelma?.motivaatiokirjeAsiakirja ?: return assembler
        val data = motivaatiokirje.asiakirjaData?.data
        if (
            motivaatiokirje.tyyppi != MediaType.APPLICATION_PDF_VALUE ||
            data == null ||
            !pdfContentValidator.isValid(data)
        ) {
            assembler.close()
            throw InvalidPdfAttachmentException(
                attachmentId = motivaatiokirje.id,
                attachmentName = motivaatiokirje.nimi,
                source = InvalidPdfAttachmentSource.MOTIVAATIOKIRJE
            )
        }
        try {
            assembler.add(data)
        } catch (e: Exception) {
            assembler.close()
            throw InvalidPdfAttachmentException(
                attachmentId = motivaatiokirje.id,
                attachmentName = motivaatiokirje.nimi,
                source = InvalidPdfAttachmentSource.MOTIVAATIOKIRJE,
                cause = e
            )
        }
        return assembler
    }

    private fun lisaaPaivakirjamerkinnat(
        opintooikeusId: Long,
        assembler: PdfAssembler
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
        assembler.add(paivakirjamerkinnatStream)
    }

    private fun lisaaSeurantajaksot(
        opintooikeusId: Long,
        valmistumispyynto: Valmistumispyynto,
        assembler: PdfAssembler
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
            assembler.add(seurantajaksoStream)
        }
    }

    private companion object {
        const val PAIVAMAARAFORMAATTI = "yyyyMMdd"
        const val MIB = 1024 * 1024
        val SUOMEN_LOCALE: Locale = Locale.forLanguageTag("fi")
    }
}
