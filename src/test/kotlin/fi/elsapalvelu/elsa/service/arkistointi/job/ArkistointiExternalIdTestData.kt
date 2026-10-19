package fi.elsapalvelu.elsa.service.arkistointi.job

internal object ArkistointiExternalIdTestData {
    fun value(kind: String): String? = when (kind) {
        "NULL" -> null
        "MAX_ASCII" -> "x".repeat(255)
        "MAX_UNICODE" -> "😀".repeat(127) + "x"
        "OPAQUE" -> "  archive:Document/123?revision=2  "
        "TOO_LONG" -> "x".repeat(256)
        "TOO_LONG_UNICODE" -> "😀".repeat(128)
        "EMPTY" -> ""
        "BLANK" -> " \t\n"
        "NUL" -> "private-response\u0000id"
        "HIGH_SURROGATE" -> "private-response\uD800"
        "LOW_SURROGATE" -> "private-response\uDC00"
        else -> error("Unknown external ID test case")
    }
}
