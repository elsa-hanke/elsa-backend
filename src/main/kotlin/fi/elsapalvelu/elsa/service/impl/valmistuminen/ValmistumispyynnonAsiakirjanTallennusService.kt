package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Persists a generated PDF as an [Asiakirja] of the graduation request's study right and links it
 * to the graduation request.
 */
@Service
class ValmistumispyynnonAsiakirjanTallennusService(
    private val asiakirjaRepository: AsiakirjaRepository,
    private val valmistumispyyntoRepository: ValmistumispyyntoRepository
) {

    /**
     * Saves [data] as `<tiedostonimenAlku>_yyyyMMdd.pdf`, lets [kiinnita] attach the saved
     * document to the right field of [valmistumispyynto] and then saves [valmistumispyynto].
     */
    fun tallenna(
        valmistumispyynto: Valmistumispyynto,
        tiedostonimenAlku: String,
        data: ByteArray,
        kiinnita: (Valmistumispyynto, Asiakirja) -> Unit
    ): Asiakirja {
        val aikaleima = LocalDate.now().format(DateTimeFormatter.ofPattern(PAIVAMAARAFORMAATTI))
        val asiakirja = asiakirjaRepository.save(
            Asiakirja(
                opintooikeus = valmistumispyynto.opintooikeus,
                nimi = "${tiedostonimenAlku}_${aikaleima}.pdf",
                tyyppi = MediaType.APPLICATION_PDF_VALUE,
                lisattypvm = LocalDateTime.now(),
                asiakirjaData = AsiakirjaData(data = data)
            )
        )
        kiinnita(valmistumispyynto, asiakirja)
        valmistumispyyntoRepository.save(valmistumispyynto)
        return asiakirja
    }

    private companion object {
        const val PAIVAMAARAFORMAATTI = "yyyyMMdd"
    }
}
