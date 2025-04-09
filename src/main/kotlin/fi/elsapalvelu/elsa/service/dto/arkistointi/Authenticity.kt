package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.time.LocalDate

class Authenticity {

    @JacksonXmlProperty(localName = "Checker")
    var checker: String? = null

    @JacksonXmlProperty(localName = "Date")
    var date: LocalDate? = null

    @JacksonXmlProperty(localName = "Description")
    var description: String? = null
}
