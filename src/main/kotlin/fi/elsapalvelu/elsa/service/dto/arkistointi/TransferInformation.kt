package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class TransferInformation {

    @JacksonXmlProperty(localName = "NativeId")
    var nativeId: String? = null

    @JacksonXmlProperty(localName = "Title")
    var title: String? = "ELSA"

    @JacksonXmlProperty(localName = "UseType")
    var useType: String? = "Arkisto"

    @JacksonXmlProperty(localName = "TransferContractId")
    var transferContractId: String? = null

    @JacksonXmlProperty(localName = "MetadataSchema")
    var metadataSchema: String? = "http://www.arkisto.fi/skeemat/Sahke2_2019_03.xsd"
}
