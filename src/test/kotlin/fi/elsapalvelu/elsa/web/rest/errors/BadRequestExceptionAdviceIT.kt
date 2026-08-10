package fi.elsapalvelu.elsa.web.rest.errors

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.security.ADMIN
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
@WithMockUser(authorities = [ADMIN])
class BadRequestExceptionAdviceIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun validationFailureReturnsProblemDetailContract() {
        mockMvc.perform(
            post("$ENDPOINT_BASE_URL/method-argument")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Invalid request content."))
            .andExpect(jsonPath("$.instance").value("$ENDPOINT_BASE_URL/method-argument"))
    }

    @Test
    fun accessDeniedReturnsProblemDetailContract() {
        mockMvc.perform(get("$ENDPOINT_BASE_URL/access-denied"))
            .andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("test access denied!"))
            .andExpect(jsonPath("$.instance").value("$ENDPOINT_BASE_URL/access-denied"))
    }

    @Test
    fun unsupportedMethodReturnsProblemDetailContract() {
        mockMvc.perform(get("$ENDPOINT_BASE_URL/method-argument"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Method Not Allowed"))
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.detail").value("Method 'GET' is not supported."))
            .andExpect(jsonPath("$.instance").value("$ENDPOINT_BASE_URL/method-argument"))
    }

    @Test
    fun unexpectedExceptionReturnsProblemDetailContract() {
        mockMvc.perform(get("$ENDPOINT_BASE_URL/internal-server-error"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").doesNotExist())
            .andExpect(jsonPath("$.instance").value("$ENDPOINT_BASE_URL/internal-server-error"))
    }

    companion object {
        private const val ENDPOINT_BASE_URL = "/api/exception-translator-test"
    }
}
