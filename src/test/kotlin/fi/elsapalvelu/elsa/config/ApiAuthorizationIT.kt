package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ApiAuthorizationIT {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var requestMappings: RequestMappingHandlerMapping

    @Test
    fun everyProtectedApiMappingRejectsAnonymousRequests() {
        TestSecurityContextHolder.clearContext()
        val routes = requestMappings.handlerMethods.flatMap { (mapping, handler) ->
            if (!handler.beanType.packageName.startsWith("fi.elsapalvelu.elsa.web.rest")) {
                emptyList()
            } else {
                val methods = mapping.methodsCondition.methods.ifEmpty { setOf(RequestMethod.GET) }
                mapping.patternValues.flatMap { path -> methods.map { method -> method to path } }
            }
        }

        assertThat(routes).isNotEmpty
        routes.forEach { (method, pattern) ->
            assertThat(pattern).describedAs("API mapping: $method $pattern")
                .matches { it == "/" || it == "/kirjaudu" || it == "/api" || it.startsWith("/api/") }

            if (isPublic(pattern)) return@forEach

            val path = pattern.replace(Regex("\\{[^}]+}"), "1")
                .replace("**", "1")
                .replace("*", "1")
            val request = request(HttpMethod.valueOf(method.name), path).with(anonymous())
            if (method !in setOf(RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS)) {
                request.with(csrf())
            }

            val response = mockMvc.perform(request).andReturn().response
            System.out.println("Anonymous $method $pattern ($path) -> ${response.status}")
            assertThat(response.status).describedAs("Anonymous $method $pattern")
                .isEqualTo(401)
        }
    }

    @Test
    fun publicRoutesStayPublic() {
        TestSecurityContextHolder.clearContext()
        mockMvc.perform(get("/api/ping").with(anonymous())).andExpect(status().isOk)
        mockMvc.perform(get("/api/julkinen/seuraava-paivitys").with(anonymous()))
            .andExpect(status().isOk)
    }

    @Test
    fun oneRoleCannotReadAnotherRolesApi() {
        TestSecurityContextHolder.clearContext()
        val roleEndpoints = mapOf(
            ERIKOISTUVA_LAAKARI to "/api/erikoistuva-laakari/suoritusarvioinnit-rajaimet",
            YEK_KOULUTETTAVA to "/api/yek-koulutettava/valmistumispyynto",
            KOULUTTAJA to "/api/kouluttaja/etusivu/erikoistujien-seuranta-rajaimet",
            VASTUUHENKILO to "/api/vastuuhenkilo/terveyskeskuskoulutusjaksot",
            TEKNINEN_PAAKAYTTAJA to "/api/tekninen-paakayttaja/erikoisalat",
            OPINTOHALLINNON_VIRKAILIJA to
                "/api/virkailija/etusivu/erikoistujien-seuranta-rajaimet"
        )
        val registeredPaths = requestMappings.handlerMethods.keys.flatMap { it.patternValues }
        assertThat(registeredPaths).containsAll(roleEndpoints.values)

        roleEndpoints.forEach { (allowedRole, path) ->
            roleEndpoints.keys.filter { it != allowedRole }.forEach { otherRole ->
                val response = mockMvc.perform(
                    get(path).with(user("test-user").authorities(SimpleGrantedAuthority(otherRole)))
                ).andReturn().response
                System.out.println("$otherRole GET $path -> ${response.status}")
                assertThat(response.status).describedAs("$otherRole must not access $path")
                    .isEqualTo(403)
            }
        }
    }

    @Test
    fun oneRoleCannotWriteAnotherRolesApi() {
        TestSecurityContextHolder.clearContext()
        val writeEndpoints = listOf(
            Triple(HttpMethod.POST, "/api/yek-koulutettava/valmistumispyynto", YEK_KOULUTETTAVA),
            Triple(HttpMethod.PUT, "/api/vastuuhenkilo/terveyskeskuskoulutusjakson-hyvaksynta/{id}", VASTUUHENKILO),
            Triple(HttpMethod.DELETE, "/api/tekninen-paakayttaja/ilmoitukset/{id}", TEKNINEN_PAAKAYTTAJA)
        )
        val registeredMethodsAndPaths = requestMappings.handlerMethods.keys.flatMap { mapping ->
            mapping.patternValues.flatMap { path -> mapping.methodsCondition.methods.map { method -> method.name to path } }
        }
        assertThat(registeredMethodsAndPaths).containsAll(
            writeEndpoints.map { (method, path) -> method.name() to path }
        )

        writeEndpoints.forEach { (method, pattern, allowedRole) ->
            val path = pattern.replace("{id}", "1")
            listOf(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA, KOULUTTAJA, VASTUUHENKILO,
                TEKNINEN_PAAKAYTTAJA, OPINTOHALLINNON_VIRKAILIJA)
                .filter { it != allowedRole }
                .forEach { otherRole ->
                    val response = mockMvc.perform(
                        request(method, path)
                            .with(user("test-user").authorities(SimpleGrantedAuthority(otherRole)))
                            .with(csrf())
                    ).andReturn().response
                    System.out.println("$otherRole $method $pattern -> ${response.status}")
                    assertThat(response.status).describedAs("$otherRole must not access $method $pattern")
                        .isEqualTo(403)
                }
        }
    }

    @Test
    fun authenticatedWritesRequireCsrfToken() {
        TestSecurityContextHolder.clearContext()
        val endpoint = "/api/yek-koulutettava/valmistumispyynto"
        val authenticated = user("test-user").authorities(SimpleGrantedAuthority(YEK_KOULUTETTAVA))

        mockMvc.perform(request(HttpMethod.POST, endpoint).with(authenticated))
            .andExpect(status().isForbidden)
        // Invalid CSRF must be rejected before the controller can write anything.
        mockMvc.perform(request(HttpMethod.POST, endpoint).with(authenticated).with(csrf().useInvalidToken()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun impersonationAndAuthInfoAreRestrictedAtFilterChain() {
        TestSecurityContextHolder.clearContext()
        mockMvc.perform(get("/api/auth-info").with(anonymous())).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/auth-info")
            .with(user("test-user").authorities(SimpleGrantedAuthority(VASTUUHENKILO))))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/login/impersonate")
            .with(user("test-user").authorities(SimpleGrantedAuthority(YEK_KOULUTETTAVA))))
            .andExpect(status().isForbidden)
    }

    private fun isPublic(path: String): Boolean =
        path == "/" || path == "/kirjaudu" || path == "/api/" ||
            path == "/api/ping" || path == "/api/haka-yliopistot" ||
            path.startsWith("/api/julkinen/")
}
