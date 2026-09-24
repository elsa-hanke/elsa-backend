package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.LIITE
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.YHTEENVETO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonAsiakirjojenLuontiService(
    private val yhteenvetoPdfService: ValmistumispyynnonYhteenvetoPdfService,
    private val liitteetPdfService: ValmistumispyynnonLiitteetPdfService,
    private val erikoistujanTiedotPdfService: ValmistumispyynnonErikoistujanTiedotPdfService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun luo(
        tarkistus: ValmistumispyynnonTarkistusDTO,
        valmistumispyynto: Valmistumispyynto
    ): List<RecordProperties> {
        val yek = valmistumispyynto.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID
        if (yek) {
            val yhteenveto = mittaa("yhteenveto (YEK)") {
                yhteenvetoPdfService.luoYek(tarkistus, valmistumispyynto)
            }
            val liitteet = mittaa("liitteet") { liitteetPdfService.luo(valmistumispyynto) }
            return luoArkistointitiedot(yhteenveto, liitteet)
        }

        val liitteet = mittaa("liitteet") { liitteetPdfService.luo(valmistumispyynto) }
        mittaa("erikoistujan tiedot") { erikoistujanTiedotPdfService.luo(valmistumispyynto) }
        val yhteenveto = mittaa("yhteenveto") {
            yhteenvetoPdfService.luo(tarkistus, valmistumispyynto)
        }
        return luoArkistointitiedot(yhteenveto, liitteet)
    }

    private fun <T> mittaa(vaihe: String, block: () -> T): T {
        val aloitettu = System.currentTimeMillis()
        val tulos = block()
        log.info("PDF-vaihe valmis [vaihe=$vaihe, kesto=${System.currentTimeMillis() - aloitettu} ms]")
        return tulos
    }

    private fun luoArkistointitiedot(
        yhteenveto: Asiakirja,
        liitteet: Asiakirja
    ) = listOf(
            RecordProperties(yhteenveto, YHTEENVETO),
            RecordProperties(liitteet, LIITE)
        )
}
