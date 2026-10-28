package fi.elsapalvelu.elsa.service.arkistointi.job.document

fun interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiDocumentDescriptor): LoadedArkistointiAsiakirja
}
