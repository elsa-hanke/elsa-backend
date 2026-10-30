package fi.elsapalvelu.elsa.service.arkistointi.louhi

import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkeDelivery
import fi.elsapalvelu.elsa.service.arkistointi.sahke.PreparedSahkePackage

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
@Profile("!e2e-archive")
class LouhiAsyncArkistointiAdapter(
    private val delivery: AsyncSahkeDelivery,
    private val louhi: TampereLouhiService
) : ArkistointiAdapter {
    override val university = YliopistoEnum.TAMPEREEN_YLIOPISTO
    override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)

    override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult =
        delivery.deliver(pyynto, this, "LOUHI", ::invalid, ::send)

    private fun send(archive: PreparedSahkePackage): ArkistointiResult =
        try {
            louhi.laheta(archive.path.toString(), archive.yek, LouhiExecutionPolicy.ASYNC)
            ArkistointiResult.Success(null)
        } catch (exception: LouhiTransferException) {
            val failure = exception.failure
            val error = ArkistointiError.create(failure.code,
                "Louhi-siirto epaonnistui [stage=${exception.stage.name}, reason=${exception.reason.name}, status=${exception.sftpStatus}]")
            if (failure.retryable) ArkistointiResult.RetryableFailure(error)
            else ArkistointiResult.PermanentFailure(error)
        } catch (_: ResponseStatusException) {
            retryable()
        }

    private fun invalid(code: String) = ArkistointiResult.PermanentFailure(
        ArkistointiError.create(code, "Louhi-toimituksen tiedot tai vastaanoton asetukset on tarkistettava")
    )

    private fun retryable() = ArkistointiResult.RetryableFailure(
        ArkistointiError.create("LOUHI_TRANSFER_FAILED", "Louhi-siirto ei valmistunut ennen julkaisua")
    )
}
