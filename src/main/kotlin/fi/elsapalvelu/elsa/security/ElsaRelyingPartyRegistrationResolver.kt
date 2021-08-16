package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.ApplicationProperties
import org.springframework.core.convert.converter.Converter
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.web.util.UrlUtils
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.util.UriComponentsBuilder
import java.util.function.Function
import javax.servlet.http.HttpServletRequest

private const val PATH_DELIMITER = '/'

@Component
class ElsaRelyingPartyRegistrationResolver(
    private val relyingPartyRegistrationRepository: RelyingPartyRegistrationRepository? = null,
    private val applicationProperties: ApplicationProperties
) :
    Converter<HttpServletRequest, RelyingPartyRegistration> {

    private val registrationIdResolver: RegistrationIdResolver = RegistrationIdResolver

    override fun convert(request: HttpServletRequest): RelyingPartyRegistration? {
        val registrationId = registrationIdResolver.convert(request) ?: return null
        val relyingPartyRegistration = relyingPartyRegistrationRepository
            ?.findByRegistrationId(registrationId) ?: return null
        val applicationUri = getApplicationUri(request)
        val templateResolver: Function<String, String> =
            templateResolver(applicationUri, relyingPartyRegistration)
        val relyingPartyEntityId = templateResolver.apply(relyingPartyRegistration.entityId)
        val assertionConsumerServiceLocation = templateResolver
            .apply(relyingPartyRegistration.assertionConsumerServiceLocation)
        return RelyingPartyRegistration.withRelyingPartyRegistration(relyingPartyRegistration)
            .entityId(relyingPartyEntityId)
            .assertionConsumerServiceLocation(assertionConsumerServiceLocation)
            .build()
    }

    private fun templateResolver(
        applicationUri: String,
        relyingParty: RelyingPartyRegistration
    ): Function<String, String> {
        return Function { template: String ->
            resolveUrlTemplate(
                template,
                applicationUri,
                relyingParty
            )
        }
    }

    private fun resolveUrlTemplate(
        template: String,
        baseUrl: String,
        relyingParty: RelyingPartyRegistration
    ): String {
        val entityId = relyingParty.assertingPartyDetails.entityId
        val registrationId = relyingParty.registrationId
        val uriVariables: MutableMap<String, String?> = HashMap()
        val uriComponents =
            UriComponentsBuilder.fromHttpUrl(baseUrl).replaceQuery(null).fragment(null)
                .build()
        val scheme = uriComponents.scheme
        uriVariables["baseScheme"] = scheme ?: ""
        val host = uriComponents.host
        uriVariables["baseHost"] = host ?: ""
        // following logic is based on HierarchicalUriComponents#toUriString()
        val port = uriComponents.port
        uriVariables["basePort"] = if (port == -1) "" else ":$port"
        var path = uriComponents.path
        if (StringUtils.hasLength(path) && path!![0] != PATH_DELIMITER) {
            path = PATH_DELIMITER.toString() + path
        }
        uriVariables["basePath"] = path ?: ""
        uriVariables["baseUrl"] = uriComponents.toUriString()
        uriVariables["entityId"] = if (StringUtils.hasText(entityId)) entityId else ""
        uriVariables["registrationId"] =
            if (StringUtils.hasText(registrationId)) registrationId else ""
        return UriComponentsBuilder.fromUriString(template).buildAndExpand(uriVariables)
            .toUriString()
    }

    private fun getApplicationUri(request: HttpServletRequest): String {
        val uriComponents = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
            .replacePath(request.contextPath).replaceQuery(null).fragment(null).build()
        return uriComponents.toUriString()
            .replace("http", applicationProperties.getSecurity().samlScheme!!)
    }

    companion object RegistrationIdResolver :
        Converter<HttpServletRequest?, String?> {

        private val requestMatcher: RequestMatcher = AntPathRequestMatcher("/**/{registrationId}")

        override fun convert(request: HttpServletRequest?): String? {
            val result = requestMatcher.matcher(request)
            return result.variables["registrationId"]
        }
    }
}
