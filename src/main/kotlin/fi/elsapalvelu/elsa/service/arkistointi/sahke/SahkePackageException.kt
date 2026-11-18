package fi.elsapalvelu.elsa.service.arkistointi.sahke

enum class SahkePackageFailure(val code: String) {
    PAYLOAD_INVALID("SAHKE_PAYLOAD_INVALID"),
    CONFIGURATION_INVALID("SAHKE_CONFIGURATION_INVALID"),
    LOCALIZATION_MISSING("SAHKE_LOCALIZATION_MISSING")
}

/** Only explicitly recognized permanent package failures; never retains input or a cause chain. */
class SahkePackageException(val failure: SahkePackageFailure) : IllegalArgumentException(failure.code)
