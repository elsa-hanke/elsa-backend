package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import java.io.Serializable

class KayttajahallintaErikoistuvaLaakariFormDTO(

    var yliopistot: MutableSet<YliopistoDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var asetukset: MutableSet<AsetusDTO> = mutableSetOf(),

    var opintooppaat: MutableSet<OpintoopasSimpleDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "KayttajahallintaErikoistuvaLaakariFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}
