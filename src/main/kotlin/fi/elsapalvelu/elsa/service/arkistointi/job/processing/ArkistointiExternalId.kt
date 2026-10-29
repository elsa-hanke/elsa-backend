package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError

/** Opaque, unmodified ID: null or nonblank valid Unicode, <=255 UTF-16 units, without NUL. */
internal object ArkistointiExternalId {
    fun validate(externalId: String?) {
        if (externalId == null) return
        if (externalId.length > ArkistointiJob.MAX_EXTERNAL_ID_LENGTH || externalId.isBlank() ||
            '\u0000' in externalId || !Charsets.UTF_8.newEncoder().canEncode(externalId)) {
            throw ArkistointiProcessingException(
                ArkistointiError.create(
                    "ARCHIVE_ADAPTER_RESPONSE_INVALID",
                    "Arkisto on voinut vastaanottaa asiakirjat, mutta sen vastaus rikkoo adapterin sopimusta. " +
                        "Toimitus on tarkistettava ennen uutta lahetysta."
                ),
                retryable = false
            )
        }
    }
}
