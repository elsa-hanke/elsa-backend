package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Organisation {

    @JacksonXmlProperty(localName = "Name")
    var name: String? = null
}
