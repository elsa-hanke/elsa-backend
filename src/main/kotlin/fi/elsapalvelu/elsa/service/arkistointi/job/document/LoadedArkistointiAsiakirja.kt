package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

data class LoadedArkistointiAsiakirja(
    val asiakirjaId: Long,
    val asiakirjatyyppi: RecordType,
    val jarjestysnumero: Int,
    val filename: String,
    val contentType: String,
    val sha256: String,
    val sisalto: ByteArray
) {
    override fun toString() = "LoadedArkistointiAsiakirja(asiakirjaId=$asiakirjaId)"
}
