package fi.elsapalvelu.elsa.service.integration

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.net.ssl.SSLException

class IntegrationAuthenticationAlertInterceptor(
    private val integrationAlertService: IntegrationAlertService,
    private val alertKey: IntegrationAlertKey,
    private val integrationName: String,
    private val authenticationFailureStatuses: Set<Int> = setOf(401),
    private val alertOnTlsFailure: Boolean = false,
    private val resetOnSuccessfulResponse: Boolean = true,
    private val suppressedByAlertKey: IntegrationAlertKey? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request).also { response ->
                when {
                    response.code in authenticationFailureStatuses && !isSuppressedByAnotherAlert() ->
                        integrationAlertService.publishOnceUntilSuccess(
                            alertKey,
                            "$integrationName opintotietointegraation autentikointi epäonnistui",
                            "$integrationName opintotietointegraatio palautti autentikointivirheen. " +
                                "HTTP status: ${response.code}. Endpoint: ${request.url}. " +
                                "Virhe ei liity yksittäiseen henkilötunnukseen."
                        )

                    response.isSuccessful && resetOnSuccessfulResponse ->
                        integrationAlertService.markSuccessful(alertKey)
                }
            }
        } catch (e: IOException) {
            if (alertOnTlsFailure && e.hasSslCause()) {
                integrationAlertService.publishOnceUntilSuccess(
                    alertKey,
                    "$integrationName opintotietointegraation TLS-yhteys epäonnistui",
                    "$integrationName opintotietointegraation TLS-yhteyden muodostaminen epäonnistui. " +
                        "Endpoint: ${request.url}. Virhe ei liity yksittäiseen henkilötunnukseen."
                )
            }
            throw e
        }
    }

    private fun isSuppressedByAnotherAlert(): Boolean =
        suppressedByAlertKey?.let { integrationAlertService.isActive(it) } == true
}

internal fun Throwable.hasSslCause(): Boolean =
    generateSequence(this) { it.cause }.any { it is SSLException }
