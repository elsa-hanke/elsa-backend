package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class ContactPerson {

    @JacksonXmlProperty(localName = "Name")
    var name: String? = null

    @JacksonXmlProperty(localName = "Address")
    var address: String? = null

    @JacksonXmlProperty(localName = "PhoneNumber")
    var phoneNumber: String? = null

    @JacksonXmlProperty(localName = "Email")
    var email: String? = null
}
