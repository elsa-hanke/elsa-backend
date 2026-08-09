package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ArkistointiDispatcher(
    adapters: List<ArkistointiAdapter>,
    private val configurationProvider: ArkistointiConfigurationProvider,
    private val alertPublisherService: AlertPublisherService,
    private val arkistointiMetrics: ArkistointiMetricsService
) {
    private val log = LoggerFactory.getLogger(ArkistointiDispatcher::class.java)
    private val adaptersByYliopisto = adapters
        .groupBy(ArkistointiAdapter::yliopisto)
        .also(::requireUniqueAdapters)
        .mapValues { (_, yliopistonAdapters) -> yliopistonAdapters.single() }

    fun laheta(request: ArkistointiDeliveryRequest) {
        arkistointiMetrics.activeArkistointiOperations.incrementAndGet()
        try {
            val adapter = resolveAdapter(request) ?: return
            adapter.laheta(request)
            arkistointiMetrics.recordSuccess(request.yliopisto, request.caseType)
        } catch (e: Exception) {
            publishFailureAlert(request, e)
            arkistointiMetrics.recordError(request.yliopisto, request.caseType)
            throw e
        } finally {
            arkistointiMetrics.activeArkistointiOperations.updateAndGet { maxOf(0, it - 1) }
        }
    }

    private fun resolveAdapter(request: ArkistointiDeliveryRequest): ArkistointiAdapter? {
        return adaptersByYliopisto[request.yliopisto] ?: if (
            configurationProvider.onKaytossa(request.yliopisto, request.caseType)
        ) {
            error(
                "Arkistointi on käytössä yliopistossa ${request.yliopisto.name} " +
                    "asiatyypille ${request.caseType.value}, mutta adapteria ei ole määritelty"
            )
        } else {
            log.info("Integraatiota arkistointiin ei ole tuettu yliopistossa ${request.yliopisto.name}")
            null
        }
    }

    private fun requireUniqueAdapters(adapters: Map<YliopistoEnum, List<ArkistointiAdapter>>) {
        val duplicateYliopistot = adapters.filterValues { it.size > 1 }.keys
        require(duplicateYliopistot.isEmpty()) {
            "Yliopistolle saa olla vain yksi arkistointiadapteri. Useita adaptereita: " +
                duplicateYliopistot.joinToString { it.name }
        }
    }

    private fun publishFailureAlert(request: ArkistointiDeliveryRequest, exception: Exception) {
        when (request.yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> alertPublisherService.publishAlert(
                subject = "Tampere arkistointi epäonnistui",
                message = buildAlertMessage(
                    intro = "Arkistointitiedoston lähetys Louhi SFTP-palvelimelle epäonnistui.",
                    extra = "SFTP-palvelin: ${configurationProvider.getTampereHost() ?: "tuntematon"}.",
                    request = request,
                    error = exception.message
                )
            )

            YliopistoEnum.HELSINGIN_YLIOPISTO -> alertPublisherService.publishAlert(
                subject = "Helsinki arkistointi epäonnistui",
                message = buildAlertMessage(
                    intro = "Arkistointitiedoston lähetys HY Siilo-palveluun epäonnistui.",
                    extra = null,
                    request = request,
                    error = exception.message
                )
            )

            else -> Unit
        }
    }

    private fun buildAlertMessage(
        intro: String,
        extra: String?,
        request: ArkistointiDeliveryRequest,
        error: String?
    ): String = buildString {
        append(intro)
        if (extra != null) append(" $extra")
        append(" CaseType: ${request.caseType.value}")
        if (request.caseId != null) append(", Id: ${request.caseId}")
        if (request.erikoistujanNimi != null) {
            append(". Erikoistuva lääkäri: ${request.erikoistujanNimi}")
        }
        append(". Tiedosto: ${request.filePath}")
        append(". Virhe: $error")
    }
}
