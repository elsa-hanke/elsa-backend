package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class File {

    @JacksonXmlProperty(localName = "Name")
    var name: String? = null

    @JacksonXmlProperty(localName = "Path")
    var path: String? = null
}
