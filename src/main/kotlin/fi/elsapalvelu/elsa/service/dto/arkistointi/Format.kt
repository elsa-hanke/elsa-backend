package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Format {

    @JacksonXmlProperty(localName = "Name")
    var name: String? = null

    @JacksonXmlProperty(localName = "Version")
    var version: String? = "1.0"
}
