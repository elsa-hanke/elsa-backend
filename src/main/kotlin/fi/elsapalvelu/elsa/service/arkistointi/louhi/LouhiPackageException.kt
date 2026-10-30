package fi.elsapalvelu.elsa.service.arkistointi.louhi

enum class LouhiPackageFailure(val code: String) {
    PAYLOAD_INVALID("LOUHI_PAYLOAD_INVALID"),
    CONFIGURATION_INVALID("LOUHI_CONFIGURATION_INVALID"),
    LOCALIZATION_MISSING("LOUHI_LOCALIZATION_MISSING")
}

/** Only explicitly recognized permanent package failures; never retains input or a cause chain. */
class LouhiPackageException(val failure: LouhiPackageFailure) : IllegalArgumentException(failure.code)
