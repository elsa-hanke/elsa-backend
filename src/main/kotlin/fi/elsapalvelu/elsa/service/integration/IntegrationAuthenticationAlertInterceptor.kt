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
    private val alertOnNetworkFailure: Boolean = false,
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
                            "$integrationName opintotietointegraation autentikointi eponnistui",
                            "$integrationName opintotietointegraatio palautti autentikointivirheen. " +
                                "HTTP status: ${response.code}. Endpoint: ${request.url}. " +
                                "Virhe ei liity yksittiseen henkiltunnukseen."
                        )

                    response.isSuccessful && resetOnSuccessfulResponse ->
                        integrationAlertService.markSuccessful(alertKey)
                }
            }
        } catch (e: IOException) {
            val isSsl = e.hasSslCause()
            when {
                isSsl && alertOnTlsFailure ->
                    integrationAlertService.publishOnceUntilSuccess(
                        alertKey,
                        "$integrationName opintotietointegraation TLS-yhteys eponnistui",
                        "$integrationName opintotietointegraation TLS-yhteyden muodostaminen eponnistui. " +
                            "Endpoint: ${request.url}. Virhe ei liity yksittiseen henkiltunnukseen."
                    )
                !isSsl && alertOnNetworkFailure ->
                    integrationAlertService.publishOnceUntilSuccess(
                        alertKey,
                        "$integrationName opintotietointegraatioon ei saatu yhteyttä",
                        "$integrationName opintotietointegraatioon yhdistäminen eponnistui. " +
                            "Endpoint: ${request.url}. Virhe: ${e.message}. " +
                            "Virhe ei liity yksittiseen henkiltunnukseen."
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
