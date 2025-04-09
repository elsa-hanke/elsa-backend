package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class ClassificationScheme {

    @JacksonXmlProperty(localName = "MainFunction")
    var mainFunction: MainFunction = MainFunction()
}
