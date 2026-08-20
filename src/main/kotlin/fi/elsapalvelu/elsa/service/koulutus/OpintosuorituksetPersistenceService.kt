package fi.elsapalvelu.elsa.service.koulutus

import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO

interface OpintosuorituksetPersistenceService {

    fun createOrUpdateIfChanged(userId: String, opintosuoritukset: OpintosuorituksetPersistenceDTO)
}
