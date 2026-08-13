package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.arviointi.Arviointiasteikko
import fi.elsapalvelu.elsa.domain.arviointi.ArviointiasteikonTaso
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoriteWithSuoritemerkinnatDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaWithSuoritemerkinnatDTO
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritemerkintaMapper
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale

@Service
class ValmistumispyynnonSuoritemerkintaPdfService(
    private val pdfService: PdfService,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val suoritteenKategoriaRepository: SuoritteenKategoriaRepository,
    private val suoritemerkintaMapper: SuoritemerkintaMapper
) {

    fun lisaa(
        opintooikeusId: Long,
        valmistumispyynto: Valmistumispyynto,
        outputStream: ByteArrayOutputStream
    ) {
        val arviointiasteikko = valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko
        val arviointiasteikonTasot = arviointiasteikko?.tasot?.associateBy { it.taso }
        val kategoriat = luoKategoriat(opintooikeusId, valmistumispyynto)
        lisaaYhteenveto(kategoriat, arviointiasteikko, arviointiasteikonTasot, outputStream)
        lisaaSuoritemerkinnat(kategoriat, arviointiasteikonTasot, outputStream)
    }

    private fun luoKategoriat(
        opintooikeusId: Long,
        valmistumispyynto: Valmistumispyynto
    ): List<SuoritteenKategoriaWithSuoritemerkinnatDTO> {
        val suoritemerkinnatSuoritteittain = suoritemerkintaRepository
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .groupBy { it.suorite?.id }
        return suoritteenKategoriaRepository.findAllByErikoisalaId(
            valmistumispyynto.opintooikeus?.erikoisala?.id.required()
        ).sortedBy { it.nimi }.map { kategoria ->
            SuoritteenKategoriaWithSuoritemerkinnatDTO(
                id = kategoria.id,
                nimi = kategoria.nimi,
                nimiSv = kategoria.nimiSv,
                arviointiasteikko =
                valmistumispyynto.opintooikeus?.opintoopas?.arviointiasteikko?.nimi,
                suoritteet = kategoria.suoritteet.sortedBy { it.nimi }.map { suorite ->
                    SuoriteWithSuoritemerkinnatDTO(
                        id = suorite.id,
                        nimi = suorite.nimi,
                        nimiSv = suorite.nimiSv,
                        voimassaolonAlkamispaiva = suorite.voimassaolonAlkamispaiva,
                        voimassaolonPaattymispaiva = suorite.voimassaolonPaattymispaiva,
                        vaadittulkm = suorite.vaadittulkm,
                        suoritemerkinnat = suoritemerkinnatSuoritteittain[suorite.id]
                            ?.sortedByDescending { it.suorituspaiva }
                            ?.map(suoritemerkintaMapper::toDto)
                            .orEmpty()
                    )
                },
                jarjestysnumero = kategoria.jarjestysnumero
            )
        }
    }

    private fun lisaaYhteenveto(
        kategoriat: List<SuoritteenKategoriaWithSuoritemerkinnatDTO>,
        arviointiasteikko: Arviointiasteikko?,
        arviointiasteikonTasot: Map<Int?, ArviointiasteikonTaso>?,
        outputStream: ByteArrayOutputStream
    ) {
        val locale = Locale.forLanguageTag("fi")
        val yhteenvetoStream = ByteArrayOutputStream()
        pdfService.luoPdf(
            "pdf/erikoistujantiedot/suoritemerkinnat.html",
            Context(locale).apply {
                setVariable("suoritteenKategoriat", kategoriat)
                setVariable("arviointiasteikko", arviointiasteikko)
                setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
            },
            yhteenvetoStream
        )
        lisaaPdf(yhteenvetoStream, outputStream)
    }

    private fun lisaaSuoritemerkinnat(
        kategoriat: List<SuoritteenKategoriaWithSuoritemerkinnatDTO>,
        arviointiasteikonTasot: Map<Int?, ArviointiasteikonTaso>?,
        outputStream: ByteArrayOutputStream
    ) {
        val locale = Locale.forLanguageTag("fi")
        kategoriat.forEach { kategoria ->
            kategoria.suoritteet?.forEach { suorite ->
                suorite.suoritemerkinnat?.forEach { suoritemerkinta ->
                    val suoritemerkintaStream = ByteArrayOutputStream()
                    pdfService.luoPdf(
                        "pdf/erikoistujantiedot/suoritemerkinta.html",
                        Context(locale).apply {
                            setVariable("suoritemerkinta", suoritemerkinta)
                            setVariable("arviointiasteikonTasot", arviointiasteikonTasot)
                            setVariable("vaativuusTasot", VALMISTUMISPYYNNON_VAATIVUUSTASOT)
                        },
                        suoritemerkintaStream
                    )
                    lisaaPdf(suoritemerkintaStream, outputStream)
                }
            }
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
}
