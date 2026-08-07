package fi.elsapalvelu.elsa.service.dto.arkistointi

import java.time.LocalDate
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Authenticity {

    @JacksonXmlProperty(localName = "Checker")
    var checker: String? = null

    @JacksonXmlProperty(localName = "Date")
    var date: LocalDate? = LocalDate.of(1, 1, 1)

    @JacksonXmlProperty(localName = "Description")
    var description: String? = null
}
