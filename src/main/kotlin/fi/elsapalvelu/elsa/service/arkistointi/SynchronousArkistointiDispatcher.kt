package fi.elsapalvelu.elsa.service.arkistointi

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import fi.elsapalvelu.elsa.service.metrics.ArkistointiMetricsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SynchronousArkistointiDispatcher(
    adapters: List<SynchronousArkistointiAdapter>,
    private val configurationProvider: ArkistointiConfigurationProvider,
    private val alertPublisherService: AlertPublisherService,
    private val arkistointiMetrics: ArkistointiMetricsService
) {
    private val log = LoggerFactory.getLogger(SynchronousArkistointiDispatcher::class.java)
    private val adaptersByYliopisto = adapters
        .groupBy(SynchronousArkistointiAdapter::yliopisto)
        .also(::requireUniqueAdapters)
        .mapValues { (_, yliopistonAdapters) -> yliopistonAdapters.single() }

    fun laheta(request: SynchronousArkistointiDeliveryRequest) {
        arkistointiMetrics.activeArkistointiOperations.incrementAndGet()
        try {
            val adapter = resolveAdapter(request) ?: return
            adapter.laheta(request)
            arkistointiMetrics.recordSuccess(request.yliopisto, request.caseType)
        } catch (e: Exception) {
            val failure = SynchronousArkistointiException((e as? SynchronousArkistointiException)?.remoteStatus)
            log.error(
                "Synchronous archiving failed [university={}, caseType={}, caseId={}, code={}, httpStatus={}]",
                request.yliopisto, request.caseType, request.caseId, SynchronousArkistointiException.ERROR_KEY, failure.remoteStatus
            )
            arkistointiMetrics.recordError(request.yliopisto, request.caseType)
            publishFailureAlert(request, failure)
            throw failure
        } finally {
            arkistointiMetrics.activeArkistointiOperations.updateAndGet { maxOf(0, it - 1) }
        }
    }

    private fun resolveAdapter(request: SynchronousArkistointiDeliveryRequest): SynchronousArkistointiAdapter? {
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

    private fun requireUniqueAdapters(adapters: Map<YliopistoEnum, List<SynchronousArkistointiAdapter>>) {
        val duplicateYliopistot = adapters.filterValues { it.size > 1 }.keys
        require(duplicateYliopistot.isEmpty()) {
            "Yliopistolle saa olla vain yksi arkistointiadapteri. Useita adaptereita: " +
                duplicateYliopistot.joinToString { it.name }
        }
    }

    private fun publishFailureAlert(request: SynchronousArkistointiDeliveryRequest, exception: SynchronousArkistointiException) {
        when (request.yliopisto) {
            YliopistoEnum.TAMPEREEN_YLIOPISTO -> alertPublisherService.publishAlert(
                subject = "Tampere arkistointi epäonnistui",
                message = buildAlertMessage(
                    intro = "Arkistointitiedoston lähetys Louhi SFTP-palvelimelle epäonnistui.",
                    request = request,
                    status = exception.remoteStatus
                )
            )

            YliopistoEnum.HELSINGIN_YLIOPISTO -> alertPublisherService.publishAlert(
                subject = "Helsinki arkistointi epäonnistui",
                message = buildAlertMessage(
                    intro = "Arkistointitiedoston lähetys HY Siilo-palveluun epäonnistui.",
                    request = request,
                    status = exception.remoteStatus
                )
            )

            else -> Unit
        }
    }

    private fun buildAlertMessage(
        intro: String,
        request: SynchronousArkistointiDeliveryRequest,
        status: Int?
    ): String = buildString {
        append(intro)
        append(" CaseType: ${request.caseType.value}")
        if (request.caseId != null) append(", Id: ${request.caseId}")
        append(". Virhekoodi: ${SynchronousArkistointiException.ERROR_KEY}")
        status?.let { append(". HTTP status: $it") }
    }
}
