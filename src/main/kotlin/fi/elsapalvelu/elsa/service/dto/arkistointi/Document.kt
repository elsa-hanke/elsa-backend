package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class Document {

    @JacksonXmlProperty(localName = "NativeId")
    var nativeId: String? = null

    @JacksonXmlProperty(localName = "UseType")
    var useType: String? = "Natiivi"

    @JacksonXmlProperty(localName = "File")
    var file: File = File()

    @JacksonXmlProperty(localName = "Format")
    var format: Format = Format()

    @JacksonXmlProperty(localName = "HashAlgorithm")
    var hashAlgorithm: String? = "SHA-256"

    @JacksonXmlProperty(localName = "HashValue")
    var hashValue: String? = null
}
