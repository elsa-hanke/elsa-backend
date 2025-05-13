package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement


@JacksonXmlRootElement(localName = "Metadata", namespace = "http://www.arkisto.fi/skeemat/Sahke2/2019/08/29")
class ArkistointiMetadata {

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsd")
    var xmlnsXsd = "http://www.w3.org/2001/XMLSchema"

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    var xmlnsXsi = "http://www.w3.org/2001/XMLSchema-instance"

    @JacksonXmlProperty(localName = "CaseFile")
    var caseFile: CaseFile = CaseFile()

    @JacksonXmlProperty(localName = "TransferInformation")
    var transferInformation: TransferInformation = TransferInformation()

    @JacksonXmlProperty(localName = "ContactInformation")
    var contactInformation: ContactInformation = ContactInformation()
}
