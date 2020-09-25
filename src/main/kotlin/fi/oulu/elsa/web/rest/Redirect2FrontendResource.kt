package fi.oulu.elsa.web.rest

import io.github.jhipster.config.JHipsterConstants
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
class Redirect2FrontendResource() {

    @GetMapping("/")
    fun redirect2FrontendView(): RedirectView? {
        return RedirectView("http://localhost:9060/")
    }
}
