package fi.elsapalvelu.elsa.service.dto.arkistointi

import java.time.LocalDate
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Custom {

    @JacksonXmlProperty(localName = "Elsa_Henkilonimi")
    var erikoistujanNimi: String? = null

    @JacksonXmlProperty(localName = "Elsa_Erikoisala")
    var erikoisala: String? = null

    @JacksonXmlProperty(localName = "Elsa_Opiskelijanumero")
    var opiskelijanumero: String? = null

    @JacksonXmlProperty(localName = "Elsa_Syntymaaika")
    var syntymaaika: LocalDate? = null

    @JacksonXmlProperty(localName = "Elsa_Yliopisto")
    var yliopisto: String? = null

    @JacksonXmlProperty(localName = "dyn_PersonalDataCollectionReason")
    var personalDataCollectionReason: String? = "Rekisterinpitäjän lakisääteisten velvotteiden noudattaminen"

    @JacksonXmlProperty(localName = "Elsa_Tarkastuspaiva")
    var tarkastuspaiva: LocalDate? = null

    @JacksonXmlProperty(localName = "Elsa_Tarkastaja")
    var tarkastaja: String? = null

    @JacksonXmlProperty(localName = "Elsa_Hyvaksymispaiva")
    var hyvaksymispaiva: LocalDate? = null

    @JacksonXmlProperty(localName = "Elsa_Hyvaksyja")
    var hyvaksyja: String? = null

    @JacksonXmlProperty(localName = "Elsa_Asiakirjatyyppi")
    var asiakirjatyyppi: String? = null
}
