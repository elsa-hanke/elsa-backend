package fi.elsapalvelu.elsa.service.dto.arkistointi

import java.time.LocalDate
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Action {

    @JacksonXmlProperty(localName = "Created")
    var created: LocalDate? = null

    @JacksonXmlProperty(localName = "Title")
    var title: String? = null

    @JacksonXmlProperty(localName = "Type")
    var type: String? = null

    @JacksonXmlProperty(localName = "Record")
    @JacksonXmlElementWrapper(useWrapping = false)
    val record: MutableList<Record> = mutableListOf()
}
