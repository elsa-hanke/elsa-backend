package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Restriction {
    @JacksonXmlProperty(localName = "PublicityClass")
    var publicityClass: String? = "Julkinen"

    @JacksonXmlProperty(localName = "PersonalData")
    var personalData: String? = "sisältää henkilötietoja"

    @JacksonXmlProperty(localName = "Person")
    var person: Person = Person()

    @JacksonXmlProperty(localName = "SecurityPeriod")
    var securityPeriod: String? = "100"

    @JacksonXmlProperty(localName = "SecurityReason")
    var securityReason: String? = ""

    @JacksonXmlProperty(localName = "SecurityClass")
    var securityClass: String? = "Ei turvallisuusluokiteltu"

    @JacksonXmlProperty(localName = "Owner")
    var owner: String? = ""
}
