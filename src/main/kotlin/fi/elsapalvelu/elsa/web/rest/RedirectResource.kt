package fi.elsapalvelu.elsa.web.rest

import io.github.jhipster.config.JHipsterConstants
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletRequest

@RestController
class RedirectResource(private val env: Environment) {

    @GetMapping("/")
    fun redirect2FrontendView(request: HttpServletRequest): RedirectView? {
        // Tarvitaan vain localhostia varten. ALB hoitaa uudelleenohjaukset AWS:ssä.
        val activeProfiles = env.activeProfiles
        return if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)) {
            RedirectView("http://localhost:9060/")
        } else {
            val url = request.requestURL.toString()
            RedirectView(url.replace("api.", ""))
        }
    }

    @PostMapping("/api")
    fun redirect2FrontendViewPost(request: HttpServletRequest): RedirectView? {
        // Tarvitaan vain localhostia varten. ALB hoitaa uudelleenohjaukset AWS:ssä.
        val activeProfiles = env.activeProfiles
        return if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)) {
            RedirectView("http://localhost:9060/")
        } else {
            val scheme = request.scheme
            val server = request.serverName
            RedirectView(scheme + "://" + server.replace("api.", ""))
        }
    }
}
