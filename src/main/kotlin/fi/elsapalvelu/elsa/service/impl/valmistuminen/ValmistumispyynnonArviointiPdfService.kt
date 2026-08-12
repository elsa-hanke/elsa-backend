package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale

@Service
class ValmistumispyynnonArviointiPdfService(
    private val pdfService: PdfService,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper,
    private val arviointitietoService: ValmistumispyynnonArviointitietoService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lisaa(
        opintooikeusId: Long,
        valmistumispyynto: Valmistumispyynto,
        outputStream: ByteArrayOutputStream
    ) {
        val arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko
        val arviointiasteikonTasot = arviointiasteikko?.tasot?.associateBy { it.taso }
        val locale = Locale.forLanguageTag("fi")
        val yhteenvetoStream = ByteArrayOutputStream()
        pdfService.luoPdf(
            "pdf/erikoistujantiedot/arvioinnit.html",
            Context(locale).apply {
                setVariable(
                    "arvioinninKategoriat",
                    arviointitietoService.haeKategoriat(opintooikeusId, false)
                )
                setVariable("arviointiasteikko", arviointiasteikko)
                setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
            },
            yhteenvetoStream
        )
        lisaaPdf(yhteenvetoStream, outputStream)

        suoritusarviointiRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .sortedWith(arviointiComparator())
            .forEach { arviointi ->
                val arviointiStream = ByteArrayOutputStream()
                pdfService.luoPdf(
                    "pdf/erikoistujantiedot/arviointi.html",
                    Context(locale).apply {
                        setVariable("arviointi", suoritusarviointiMapper.toDto(arviointi))
                        setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
                        setVariable("vaativuusTasot", VALMISTUMISPYYNNON_VAATIVUUSTASOT)
                    },
                    arviointiStream
                )
                lisaaPdf(arviointiStream, outputStream)

                yhdistaPdfAsiakirjat(
                    arviointi.arviointiAsiakirjat,
                    outputStream,
                    "Arviointiasiakirja"
                )
                yhdistaPdfAsiakirjat(
                    arviointi.itsearviointiAsiakirjat,
                    outputStream,
                    "Itsearviointiasiakirja"
                )
            }
    }

    private fun arviointiComparator() = compareBy<Suoritusarviointi>(
        {
            it.arvioitavatKokonaisuudet.minOf { kokonaisuus ->
                kokonaisuus.arvioitavaKokonaisuus?.kategoria?.nimi.required()
            }
        },
        {
            it.arvioitavatKokonaisuudet.minOf { kokonaisuus ->
                kokonaisuus.arvioitavaKokonaisuus?.nimi.required()
            }
        }
    ).thenByDescending { it.tapahtumanAjankohta }

    private fun lisaaPdf(
        newDocument: ByteArrayOutputStream,
        outputStream: ByteArrayOutputStream
    ) {
        val existingPdf = ByteArrayInputStream(outputStream.toByteArray())
        val newPdf = ByteArrayInputStream(newDocument.toByteArray())
        outputStream.reset()
        pdfService.yhdistaPdf(existingPdf, newPdf, outputStream)
    }

    private fun yhdistaPdfAsiakirjat(
        asiakirjat: Collection<Asiakirja>,
        outputStream: ByteArrayOutputStream,
        label: String
    ) {
        asiakirjat.forEach { asiakirja ->
            if (asiakirja.tyyppi != MediaType.APPLICATION_PDF_VALUE) {
                log.warn(
                    "$label ${asiakirja.id} (${asiakirja.nimi}) tyyppiä " +
                        "'${asiakirja.tyyppi}' ei tueta PDF-yhdistelyyn – ohitetaan"
                )
                return@forEach
            }
            val data = asiakirja.asiakirjaData?.data
            if (data == null || data.isEmpty()) {
                log.warn(
                    "$label ${asiakirja.id} (${asiakirja.nimi}) data on tyhjä tai null – ohitetaan"
                )
                return@forEach
            }
            val existingPdf = ByteArrayInputStream(outputStream.toByteArray())
            outputStream.reset()
            pdfService.yhdistaPdf(existingPdf, ByteArrayInputStream(data), outputStream)
        }
    }
}
