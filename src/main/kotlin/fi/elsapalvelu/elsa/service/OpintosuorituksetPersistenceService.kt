package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO

interface OpintosuorituksetPersistenceService {

    fun createOrUpdateIfChanged(userId: String, opintosuoritukset: OpintosuorituksetPersistenceDTO)
}
