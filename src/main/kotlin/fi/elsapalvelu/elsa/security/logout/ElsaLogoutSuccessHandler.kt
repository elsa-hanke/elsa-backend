package fi.elsapalvelu.elsa.security.logout

import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.util.HtmlUtils
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * <samlp:LogoutRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
Destination="https://testi.apro.tunnistus.fi/idp/profile/SAML2/Redirect/SLO"
ID="_7891c8499e749afa27c3b375091d69e9"
IssueInstant="2017-07-20T07:36:20Z"
Version="2.0"
>
<saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">https://kalastus.mallikunta.fi</saml:Issuer>
<saml2:NameID xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient" NameQualifier="https://testi.apro.tunnistus.fi/idp1" SPNameQualifier="https://kalastus.mallikunta.fi/SAML2SP" >AAdzZWNyZXQxuCv8NAYSuCyZcoBq5b9XNIRKipe09Kkscf6irTP/LWxperqMASdFTs9cn3BrRqJS/wSoK5czfvX3Xza7SC6240NmYQ8jJqKl+IThwMcFhpYt/2yDLfKGEL4mWrD72b+7IOcv8oFaZAR7gUZX2i/qLdBka54FONQ82fxpla3COg==</saml2:NameID>
<samlp:SessionIndex>_dd899f81ed9539baff725db3c5529a74</samlp:SessionIndex>
</samlp:LogoutRequest>
RelayState: ss:mem:7225343aa85efec6d77b1e64f5297f92c0f46fc09954cefc817a48ae5204ed30
SigAlg: http://www.w3.org/2001/04/xmldsig-more#rsa-sha256
Signature: HÖPÖHÖÖaTFD870iqCvZjgpGy/R1KA7r4y7Amo4GwBz5PmOeFJ/Ra8Dv7+roZoMak3PYLhBjSk17o4RIEcbioRJUNhaSqsiw/YjHA1gYz2i/JQKAfSzo7L7VKh7uOuM7niBaaKcsOKDhsJoYUUmOPZj2MbGEqnaqX6YUilf/5aN8tXFqU6f7sA35emMoGHWGNzI5ZNFjuTee/nVlmmO57Sn8yoJ6cCBm1Yf+i9Mtmwro6Fsfa0zRB0Otz+WHMOeki+4pdHefPRF5msQ2s6yUT34Wpb+eodWR2Q/sqrAjp6tdWjW2thyPdHmFen8OZss8axfhSiaybj62De0QKXNOn4A==
 */

@Component
class ElsaLogoutSuccessHandler(
    private val logoutRequestResolver: OpenSamlLogoutRequestResolver
) : LogoutSuccessHandler {

    private val logoutRequestRepository: Saml2LogoutRequestRepository =
        HttpSessionLogoutRequestRepository()
    private val redirectStrategy: RedirectStrategy = DefaultRedirectStrategy()

    override fun onLogoutSuccess(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authentication: Authentication?
    ) {
        val builder: Saml2LogoutRequestResolver.Saml2LogoutRequestBuilder<*> =
            this.logoutRequestResolver.resolveLogoutRequest(request, authentication)
        val logoutRequest = builder.logoutRequest()
        this.logoutRequestRepository.saveLogoutRequest(logoutRequest, request, response)
        if (logoutRequest!!.binding == Saml2MessageBinding.REDIRECT) {
            doRedirect(request!!, response!!, logoutRequest)
        } else {
            doPost(response!!, logoutRequest)
        }
    }

    @Throws(IOException::class)
    private fun doRedirect(
        request: HttpServletRequest, response: HttpServletResponse,
        logoutRequest: Saml2LogoutRequest
    ) {
        val location: String = logoutRequest.location
        val uriBuilder = UriComponentsBuilder.fromUriString(location)
        addParameter("SAMLRequest", logoutRequest, uriBuilder)
        addParameter("RelayState", logoutRequest, uriBuilder)
        addParameter("SigAlg", logoutRequest, uriBuilder)
        addParameter("Signature", logoutRequest, uriBuilder)
        redirectStrategy.sendRedirect(request, response, uriBuilder.build(true).toUriString())
    }

    private fun addParameter(
        name: String,
        logoutRequest: Saml2LogoutRequest,
        builder: UriComponentsBuilder
    ) {
        Assert.hasText(name, "name cannot be empty or null")
        if (StringUtils.hasText(logoutRequest.getParameter(name))) {
            builder.queryParam(
                UriUtils.encode(name, StandardCharsets.ISO_8859_1),
                UriUtils.encode(logoutRequest.getParameter(name)!!, StandardCharsets.ISO_8859_1)
            )
        }
    }

    @Throws(IOException::class)
    private fun doPost(response: HttpServletResponse, logoutRequest: Saml2LogoutRequest) {
        val html = createSamlPostRequestFormData(logoutRequest)
        response.contentType = MediaType.TEXT_HTML_VALUE
        response.writer.write(html)
    }

    private fun createSamlPostRequestFormData(logoutRequest: Saml2LogoutRequest): String {
        val location = logoutRequest.location
        val samlRequest: String? = logoutRequest.getSamlRequest()
        val relayState = logoutRequest.getRelayState()
        val html = StringBuilder()
        html.append("<!DOCTYPE html>\n")
        html.append("<html>\n").append("    <head>\n")
        html.append("        <meta charset=\"utf-8\" />\n")
        html.append("    </head>\n")
        html.append("    <body onload=\"document.forms[0].submit()\">\n")
        html.append("        <noscript>\n")
        html.append("            <p>\n")
        html.append("                <strong>Note:</strong> Since your browser does not support JavaScript,\n")
        html.append("                you must press the Continue button once to proceed.\n")
        html.append("            </p>\n")
        html.append("        </noscript>\n")
        html.append("        \n")
        html.append("        <form action=\"")
        html.append(location)
        html.append("\" method=\"post\">\n")
        html.append("            <div>\n")
        html.append("                <input type=\"hidden\" name=\"SAMLRequest\" value=\"")
        html.append(HtmlUtils.htmlEscape(samlRequest!!))
        html.append("\"/>\n")
        if (StringUtils.hasText(relayState)) {
            html.append("                <input type=\"hidden\" name=\"RelayState\" value=\"")
            html.append(HtmlUtils.htmlEscape(relayState!!))
            html.append("\"/>\n")
        }
        html.append("            </div>\n")
        html.append("            <noscript>\n")
        html.append("                <div>\n")
        html.append("                    <input type=\"submit\" value=\"Continue\"/>\n")
        html.append("                </div>\n")
        html.append("            </noscript>\n")
        html.append("        </form>\n")
        html.append("        \n")
        html.append("    </body>\n")
        html.append("</html>")
        return html.toString()
    }
}
