package fi.elsapalvelu.elsa.service.dto.arkistointi

data class ArkistointiResult(
    val zipFilePath: String,
    val metadataBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArkistointiResult

        if (zipFilePath != other.zipFilePath) return false
        if (!metadataBytes.contentEquals(other.metadataBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = zipFilePath.hashCode()
        result = 31 * result + (metadataBytes?.contentHashCode() ?: 0)
        return result
    }
}
