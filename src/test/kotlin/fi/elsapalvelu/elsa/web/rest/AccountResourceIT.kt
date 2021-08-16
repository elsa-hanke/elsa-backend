package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.security.ADMIN
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@AutoConfigureMockMvc
@WithMockUser(value = TEST_USER_LOGIN)
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class AccountResourceIT {

    @Autowired
    private lateinit var restAccountMockMvc: MockMvc

    @Test
    @Transactional
    @Throws(Exception::class)
    fun testGetExistingAccount() {
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ADMIN))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal("sub", userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        restAccountMockMvc.perform(
            get("/api/kayttaja")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("\$.authorities").value(ADMIN))
    }
}
