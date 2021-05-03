package fi.elsapalvelu.elsa.web.rest.errors

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@ControllerAdvice
class FileSizeExceptionAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleFileSizeException(
        e: MaxUploadSizeExceededException,
        redirectAttributes: RedirectAttributes
    ): String {
        redirectAttributes.addFlashAttribute("error", "File is too big")
        return "redirect:/asiakirjat"
    }
}
