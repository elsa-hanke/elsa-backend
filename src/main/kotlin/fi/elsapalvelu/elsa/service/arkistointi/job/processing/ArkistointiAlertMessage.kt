package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType

/** Only fixed transport diagnostics belong in alerts, never arbitrary exception messages or payloads. */
internal object ArkistointiAlertMessage {
    private val louhiDetails = Regex(
        "Louhi-siirto epaonnistui \\[stage=(CONNECT|LIST|WRITE|RENAME), " +
            "reason=(TIMEOUT|AUTHENTICATION_FAILED|CONNECTION_REFUSED|CONNECTION_LOST|SFTP_REJECTION|IO_FAILURE|UNKNOWN), " +
            "status=(null|[0-9]{1,3})\\]"
    )
    private val siiloDetails = Regex(
        "Siilo-toimitus epaonnistui \\[status=(null|[0-9]{3}), category=(null|TIMEOUT|DNS|CONNECT|TLS|IO)\\]"
    )
    private val safeUserId = Regex("[a-zA-Z0-9@._-]{1,100}")

    fun diagnostics(event: ArkistointiAttemptCompleted): String {
        val description = event.errorDescription.orEmpty()
        louhiDetails.matchEntire(description)?.let {
            val (stage, reason, status) = it.destructured
            return "$stage / $reason" + if (status == "null") "" else " / SFTP $status"
        }
        siiloDetails.matchEntire(description)?.let {
            val (status, category) = it.destructured
            return listOfNotNull(
                status.takeUnless { value -> value == "null" }?.let { value -> "HTTP $value" },
                category.takeUnless { value -> value == "null" }
            ).joinToString(" / ").ifEmpty { "Ei lisätietoja" }
        }
        return "Ei lisätietoja"
    }

    fun subject(event: ArkistointiAttemptCompleted, terminal: Boolean): String =
        "${university(event.university)}: arkistointi " + if (terminal) "vaatii toimenpiteitä" else "viivästyy"

    fun body(event: ArkistointiAttemptCompleted, code: String): String = """
        Yliopisto: ${university(event.university)}
        Arkistointipalvelu: ${transport(event.university)}
        Asia: ${caseName(event.caseType)}
        Asian tunniste: ${event.caseId ?: "Ei tiedossa"}
        Opinto-oikeuden tunniste: ${event.opintooikeusId ?: "Ei tiedossa"}

        Yritys: ${event.attempt}
        Virhekoodi: $code
        Virheen tiedot: ${diagnostics(event)}

        Arkistointityön tunniste: ${event.jobId}
        Hyväksyjän käyttäjätunniste: ${event.requestedByUserId?.takeIf { safeUserId.matches(it) } ?: "Ei tiedossa"}
    """.trimIndent()

    private fun university(value: YliopistoEnum): String = when (value) {
        YliopistoEnum.HELSINGIN_YLIOPISTO -> "Helsingin yliopisto"
        YliopistoEnum.TAMPEREEN_YLIOPISTO -> "Tampereen yliopisto"
        YliopistoEnum.TURUN_YLIOPISTO -> "Turun yliopisto"
        YliopistoEnum.OULUN_YLIOPISTO -> "Oulun yliopisto"
        YliopistoEnum.ITA_SUOMEN_YLIOPISTO -> "Itä-Suomen yliopisto"
    }

    private fun transport(value: YliopistoEnum): String = when (value) {
        YliopistoEnum.HELSINGIN_YLIOPISTO -> "Siilo"
        YliopistoEnum.TAMPEREEN_YLIOPISTO -> "Louhi"
        else -> "Ei tiedossa"
    }

    private fun caseName(value: CaseType): String = when (value) {
        CaseType.VALMISTUMINEN -> "Valmistumispyyntö"
        CaseType.KOEJAKSO -> "Koejakson vastuuhenkilön arvio"
        CaseType.SOPIMUS -> "Koejakson koulutussopimus"
    }
}
