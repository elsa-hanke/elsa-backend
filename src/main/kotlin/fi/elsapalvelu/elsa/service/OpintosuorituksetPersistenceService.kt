package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO

interface OpintosuorituksetPersistenceService {

    fun createOrUpdateIfChanged(userId: String, opintosuoritukset: OpintosuorituksetDTO)
}
