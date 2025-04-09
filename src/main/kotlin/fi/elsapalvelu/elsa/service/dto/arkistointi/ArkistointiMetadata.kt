package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement


@JacksonXmlRootElement(localName = "Metadata", namespace = "[http://www.arkisto.fi/skeemat/Sahke2/2019/08/29](http://www.arkisto.fi/skeemat/Sahke2/2019/08/29)")
class ArkistointiMetadata {

    @JacksonXmlProperty(localName = "CaseFile")
    var caseFile: CaseFile = CaseFile()
}
