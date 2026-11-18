package fi.elsapalvelu.elsa.service.pdf

import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.PdfTextValidator
import org.springframework.core.io.ClassPathResource

object PdfTextTestSupport {
    fun fieldValidator() = PdfTextFieldValidator(
        PdfTextValidator(
            ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
            ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
            ClassPathResource("fonts/NotoSans-Italic.ttf"),
            ClassPathResource("fonts/NotoSans-Regular.ttf")
        )
    )
}
