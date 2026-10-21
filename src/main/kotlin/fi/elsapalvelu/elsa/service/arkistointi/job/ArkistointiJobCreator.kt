package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob

interface ArkistointiJobCreator {
    /** Persist expectedDocumentCount = pyynto.asiakirjat.size and all references in the scheduling transaction. */
    fun create(pyynto: CreateArkistointiJobRequest): ArkistointiJob
}
