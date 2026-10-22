package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja

data class LoadedArkistointiAsiakirja(
    val viite: ArkistointiJobAsiakirja,
    val sisalto: ByteArray
)
