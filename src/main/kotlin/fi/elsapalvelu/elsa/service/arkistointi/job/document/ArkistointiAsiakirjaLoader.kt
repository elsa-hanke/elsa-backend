package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja

fun interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiJobAsiakirja): LoadedArkistointiAsiakirja
}
