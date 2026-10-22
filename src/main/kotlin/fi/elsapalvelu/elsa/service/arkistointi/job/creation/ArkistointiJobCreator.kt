package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob

fun interface ArkistointiJobCreator {
    /** Persist expectedDocumentCount = pyynto.asiakirjat.size and all references in the scheduling transaction. */
    fun create(pyynto: CreateArkistointiJobRequest): ArkistointiJob
}
