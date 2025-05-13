package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.time.LocalDate

class Action {

    @JacksonXmlProperty(localName = "Created")
    var created: LocalDate? = null

    @JacksonXmlProperty(localName = "Title")
    var title: String? = null

    @JacksonXmlProperty(localName = "Type")
    var type: String? = null

    @JacksonXmlProperty(localName = "Record")
    @JacksonXmlElementWrapper(useWrapping = false)
    var record: MutableList<Record> = mutableListOf()
}
