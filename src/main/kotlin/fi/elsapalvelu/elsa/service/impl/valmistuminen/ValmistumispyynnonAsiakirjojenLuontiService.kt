package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.LIITE
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType.YHTEENVETO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonAsiakirjojenLuontiService(
    private val yhteenvetoPdfService: ValmistumispyynnonYhteenvetoPdfService,
    private val liitteetPdfService: ValmistumispyynnonLiitteetPdfService,
    private val erikoistujanTiedotPdfService: ValmistumispyynnonErikoistujanTiedotPdfService
) {

    fun luo(
        tarkistus: ValmistumispyynnonTarkistusDTO,
        valmistumispyynto: Valmistumispyynto
    ): List<RecordProperties> {
        val yek = valmistumispyynto.opintooikeus?.erikoisala?.id == YEK_ERIKOISALA_ID
        if (yek) {
            val yhteenveto = yhteenvetoPdfService.luoYek(tarkistus, valmistumispyynto)
            val liitteet = liitteetPdfService.luo(valmistumispyynto)
            return luoArkistointitiedot(yhteenveto, liitteet)
        }

        val liitteet = liitteetPdfService.luo(valmistumispyynto)
        erikoistujanTiedotPdfService.luo(valmistumispyynto)
        val yhteenveto = yhteenvetoPdfService.luo(tarkistus, valmistumispyynto)
        return luoArkistointitiedot(yhteenveto, liitteet)
    }

    private fun luoArkistointitiedot(
        yhteenveto: Asiakirja,
        liitteet: Asiakirja
    ) = listOf(
            RecordProperties(yhteenveto, YHTEENVETO),
            RecordProperties(liitteet, LIITE)
        )
}
