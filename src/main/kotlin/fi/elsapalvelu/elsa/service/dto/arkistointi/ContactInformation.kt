package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class ContactInformation {

    @JacksonXmlProperty(localName = "Organisation")
    var organisation: Organisation = Organisation()

    @JacksonXmlProperty(localName = "ContactPerson")
    var contactPerson: ContactPerson = ContactPerson()
}
