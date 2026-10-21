package fi.elsapalvelu.elsa.service.arkistointi.job

interface ArkistointiAsiakirjaLoader {
    fun load(viite: ArkistointiDocumentDescriptor): LoadedArkistointiAsiakirja
}
