package fi.elsapalvelu.elsa.service.dto.arkistointi

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.time.LocalDate

class CaseFile {
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
    var retentionPeriod: String? = "-1"

    @JacksonXmlProperty(localName = "RetentionReason")
    var retentionReason: String? = "Sisältää henkilötietoja"

    @JacksonXmlProperty(localName = "Status")
    var status: String? = "Valmis"

    @JacksonXmlProperty(localName = "ClassificationScheme")
    var classificationScheme: ClassificationScheme = ClassificationScheme()

    @JacksonXmlProperty(localName = "Function")
    var function: String? = "01.02.03"

    @JacksonXmlProperty(localName = "Action")
    var action: Action = Action()
}
