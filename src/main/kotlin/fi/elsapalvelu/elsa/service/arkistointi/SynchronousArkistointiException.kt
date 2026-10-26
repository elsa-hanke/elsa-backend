package fi.elsapalvelu.elsa.service.arkistointi

/** Safe delivery failure. Never attach the transport exception or remote response as its cause. */
class SynchronousArkistointiException(val remoteStatus: Int? = null) : RuntimeException(SAFE_DESCRIPTION) {
    companion object {
        const val ERROR_KEY = "ARCHIVE_DELIVERY_FAILED"
        const val SAFE_DESCRIPTION = "Arkistointipalveluun lähettäminen epäonnistui. Ota tarvittaessa yhteyttä ELSA-palvelun tukeen."
    }
}
