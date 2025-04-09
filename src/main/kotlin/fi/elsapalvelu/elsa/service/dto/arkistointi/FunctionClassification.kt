package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class FunctionClassification {

    @JacksonXmlProperty(localName = "Title")
    var title: String? = null

    @JacksonXmlProperty(localName = "FunctionCode")
    var functionCode: String? = null

    @JacksonXmlProperty(localName = "SubFunction")
    var subFunction: SubFunction = SubFunction()
}
