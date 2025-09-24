package fi.elsapalvelu.elsa.service.dto.arkistointi

enum class PublicityClass(
    val displayName: String,
    val securityReason: String
) {
    PUBLIC(
        displayName = "Julkinen",
        securityReason = ""
    ),
    PARTIALLY_RESTRICTED(
        displayName = "Osittain salassapidettävä",
        securityReason = "JulkL 24.1"
    );
}
