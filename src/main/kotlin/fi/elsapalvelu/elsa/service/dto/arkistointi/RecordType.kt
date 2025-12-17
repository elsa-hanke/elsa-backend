package fi.elsapalvelu.elsa.service.dto.arkistointi

enum class RecordType(val displayName: String) {
    YHTEENVETO("Yhteenveto"),
    LIITE("Liite"),
    ARVIOINTI("Arviointi"),
    SOPIMUS("Sopimus")
}
