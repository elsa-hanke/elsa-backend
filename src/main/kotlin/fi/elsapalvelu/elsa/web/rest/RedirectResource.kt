package fi.elsapalvelu.elsa.web.rest

import io.github.jhipster.config.JHipsterConstants
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletRequest

@RestController
class RedirectResource(private val env: Environment) {

    @GetMapping("/")
    fun redirect2FrontendView(request: HttpServletRequest): RedirectView? {
        // TODO: Toteuta parempi tapa ohjata takaisin Web-sovellukseen
        val activeProfiles = env.activeProfiles
        return if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)) {
            RedirectView("http://localhost:9060/")
        } else {
            val url = request.requestURL.toString()
            RedirectView(url.replace("api.", ""))
        }
    }
}
