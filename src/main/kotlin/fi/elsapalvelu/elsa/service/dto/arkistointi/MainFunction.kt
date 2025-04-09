package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class MainFunction {

    @JacksonXmlProperty(localName = "Title")
    var title: String? = null

    @JacksonXmlProperty(localName = "FunctionCode")
    var functionCode: String? = null

    @JacksonXmlProperty(localName = "FunctionClassification")
    var functionClassification: FunctionClassification = FunctionClassification()
}
