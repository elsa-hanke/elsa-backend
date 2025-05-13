package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.time.LocalDate

class Record {

    @JacksonXmlProperty(localName = "Created")
    var created: LocalDate? = null

    @JacksonXmlProperty(localName = "NativeId")
    var nativeId: String? = null

    @JacksonXmlProperty(localName = "Language")
    var language: String? = "fi"

    @JacksonXmlProperty(localName = "Restriction")
    var restriction: Restriction = Restriction()

    @JacksonXmlProperty(localName = "Title")
    var title: String? = null

    @JacksonXmlProperty(localName = "RetentionPeriod")
    var retentionPeriod: String? = null

    @JacksonXmlProperty(localName = "RetentionReason")
    var retentionReason: String? = "Sisältää henkilötietoja"

    @JacksonXmlProperty(localName = "Status")
    var status: String? = "Valmis"

    @JacksonXmlProperty(localName = "Function")
    var function: String? = "04.01.04"

    @JacksonXmlProperty(localName = "Type")
    var type: String? = "Todistus"

    @JacksonXmlProperty(localName = "Authenticity")
    var authenticity: Authenticity = Authenticity()

    @JacksonXmlProperty(localName = "Document")
    var document: Document = Document()

    @JacksonXmlProperty(localName = "Custom")
    var custom: Custom = Custom()
}
