package fi.elsapalvelu.elsa.service.arkistointi.job.document

interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiDocumentDescriptor): LoadedArkistointiAsiakirja
}
