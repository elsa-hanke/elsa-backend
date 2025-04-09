package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.time.LocalDate

class Person {

    @JacksonXmlProperty(localName = "Name")
    var name: String? = null

    @JacksonXmlProperty(localName = "Ssn")
    var ssn: LocalDate? = null
}
