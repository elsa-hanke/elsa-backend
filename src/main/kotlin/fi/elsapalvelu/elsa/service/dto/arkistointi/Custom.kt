package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.time.LocalDate

class Custom {

    @JacksonXmlProperty(localName = "Sahkposti")
    var sahkposti: String? = null

    @JacksonXmlProperty(localName = "Matkapuhelin")
    var matkapuhelin: String? = null

    @JacksonXmlProperty(localName = "Yliopisto")
    var yliopisto: String? = null

    @JacksonXmlProperty(localName = "dyn_PersonalDataCollectionReason")
    var personalDataCollectionReason: String? = "Rekisterinpitäjän lakisääteisten velvotteiden noudattaminen"

    @JacksonXmlProperty(localName = "Yksikko")
    var yksikko: String? = "Lääketieteen ja terveysteknologian tiedekunta (MET)"

    @JacksonXmlProperty(localName = "Hyvaksymispaiva")
    var hyvaksymispaiva: LocalDate? = null

    @JacksonXmlProperty(localName = "Hyvaksyja")
    var hyvaksyja: String? = null
}
